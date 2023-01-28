package com.panda.agent.connection;

import com.panda.agent.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseQueryParam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClickhouseTemplate {

    private static final Logger log = LoggerFactory.getLogger(ClickhouseTemplate.class);

    private static Map<Class<?>, StatementFieldSetter> statementFieldSetterMap = new HashMap<>();

    private static StatementFieldSetter Default_Field_Setter = null;

    static {
        try {
            Class.forName("ru.yandex.clickhouse.ClickHouseDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        statementFieldSetterMap.put(int.class, (statement, i, value) -> statement.setInt(i, (Integer) value));
        statementFieldSetterMap.put(Integer.class, (statement, i, value) -> statement.setInt(i, (Integer) value));

        statementFieldSetterMap.put(long.class, (statement, i, value) -> statement.setLong(i, (Long) value));
        statementFieldSetterMap.put(Long.class, (statement, i, value) -> statement.setLong(i, (Long) value));

        statementFieldSetterMap.put(String.class, (statement, i, value) -> statement.setString(i, (String) value));
        statementFieldSetterMap.put(Date.class, (statement, i, value) -> statement.setDate(i, new java.sql.Date(((Date) value).getTime())));

        Default_Field_Setter = (statement, i, value) -> statement.setObject(i + 1, value);
    }

    private ClickHouseDataSource dataSource;
    /** SQL */
    private Map<String, String> sqlMap = new HashMap<>();
    private Map<String, String[]> sqlFieldMap = new HashMap<>();
    /** 记录当前日志已经发送的记录 */
    private Map<String, LogBatchRecord> logRecordMap = new HashMap<>();
    /** 合批发送的每一批数量上限 */
    private int maxSize;
    /** 合批发送时，每批次最大等待时间 */
    private long maxWaitTime;

    public void init() {
        String url = Configuration.getValue("jdbc.clickhouse.url");
        String db = Configuration.getValue("jdbc.clickhouse.db");
        String user = Configuration.getValue("jdbc.clickhouse.user");
        String password = Configuration.getValue("jdbc.clickhouse.password");
        this.maxSize = Configuration.getIntValue("jdbc.clickhouse.batch.maxSize", 1024);
        this.maxWaitTime = Configuration.getIntValue("jdbc.clickhouse.batch.maxWaitTime", 180);

        Properties prop = new Properties();
        prop.put(ClickHouseQueryParam.DATABASE.getKey(), db);
        prop.put(ClickHouseQueryParam.USER.getKey(), user);
        prop.put(ClickHouseQueryParam.PASSWORD.getKey(), password);
        this.dataSource = new ClickHouseDataSource(url, prop);
        log.info("create ClickHouseDataSource for url {}", url);
        log.info("ClickHouse maxSize：{}， maxWaitTime：{} ms ", maxSize, maxWaitTime);

        generateClickHouseInsertSQL();
    }

    private void generateClickHouseInsertSQL() {
        String value = Configuration.getValue("log.parser.list");
        String[] array = value.split(",");
        if (array == null || array.length == 0) {
            return;
        }

        for (String logEventType : array) {
            String tableName = Configuration.getValue("jdbc.clickhouse.table." + logEventType);
            String columns = Configuration.getValue("jdbc.clickhouse.table.columns." + logEventType);

            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            sb.append("insert into `").append(tableName).append("`(");

            String[] columnsArray = columns.split("\\|");

            boolean isFirst = true;
            List<String> fieldList = new ArrayList<>();
            for (String columnAndLogField : columnsArray) {
                if (columnAndLogField == null || "".equals(columnAndLogField)) {
                    continue;
                }

                String columnName = columnAndLogField;
                String logFieldName = columnAndLogField;

                int i = columnAndLogField.indexOf(":");
                if (i != -1) {
                    columnName = columnAndLogField.substring(0, i);
                    logFieldName = columnAndLogField.substring(i + 1);
                }
                fieldList.add(logFieldName);

                if (!isFirst) {
                    sb.append(",");
                    sb2.append(",");
                }
                sb.append("`").append(columnName).append("`");
                sb2.append("?");
                isFirst = false;
            }

            sb.append(") values (").append(sb2).append(")");

            String sql = sb.toString();
            String[] fields = fieldList.toArray(new String[fieldList.size()]);
            sqlMap.put(logEventType, sql);
            sqlFieldMap.put(logEventType, fields);

            log.info("generate SQL, logType:{}, SQL:{}", logEventType, sql);
            log.info("generate SQL, logType:{}, need logField:{}", logEventType, fields);
        }
    }

    public void addBatch(String logEventName, List<Map<String, Object>> recordList) {
        LogBatchRecord logBatchRecord = logRecordMap.get(logEventName);
        if (logBatchRecord == null) {
            logBatchRecord = new LogBatchRecord();
            logBatchRecord.setLastBatchExecuteTime(System.currentTimeMillis());

            logRecordMap.put(logEventName, logBatchRecord);
        }

        List<Map<String, Object>> batchList = logBatchRecord.getBatchList();
        batchList.addAll(recordList);

        doCheckAndBatchSend(logEventName);
    }

    public void checkAndBatchSend() {
        Set<String> logEventNames = logRecordMap.keySet();
        for (String logEventName : logEventNames) {
            doCheckAndBatchSend(logEventName);
        }
    }

    private void doCheckAndBatchSend(String logEventName) {
        LogBatchRecord logBatchRecord = logRecordMap.get(logEventName);
        if (logBatchRecord == null) {
            return;
        }

        // 检测是否达到批量发送的条件
        List<Map<String, Object>> batchList = logBatchRecord.getBatchList();
        if (batchList.size() <= 0) {
            return;
        }

        long lastBatchExecuteTime = logBatchRecord.getLastBatchExecuteTime();
        long now = System.currentTimeMillis();
        if (now - lastBatchExecuteTime > maxWaitTime || batchList.size() > maxSize) {
            batchSend(logEventName, batchList);

            logBatchRecord.setLastBatchExecuteTime(now);
            batchList.clear();
        }
    }



    private void batchSend(String logEventName, List<Map<String, Object>> recordList) {
        String sql = sqlMap.get(logEventName);
        if (sql == null) {
            throw new RuntimeException("cannot found SQL for logType:" + logEventName);
        }
        String[] fields = sqlFieldMap.get(logEventName);
        if (fields == null || sqlFieldMap.size() == 0) {
            throw new RuntimeException("cannot found log Fields for logType:" + logEventName);
        }

        ClickHouseConnection connection = null;
        PreparedStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement(sql);
            for (Map<String, Object> map : recordList) {
                for (int i = 0; i < fields.length; i++) {
                    String field = fields[i];
                    Object value = map.get(field);

                    // 设置sql字段取值
                    StatementFieldSetter setter = statementFieldSetterMap.getOrDefault(value.getClass(), Default_Field_Setter);
                    setter.setField(statement, i + 1, value);
                }
                statement.addBatch();
            }
            statement.executeBatch();

            log.info("batch send clickhouse for {}, batch size:{}", logEventName, recordList.size());
        } catch (SQLException e) {
            log.error("addBatch error", e);
        } finally {
            close(statement);
            close(connection);
        }
    }

    private void close(PreparedStatement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
            }
        }
    }

    private void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
    }

}
