package com.panda.agent.parser;

import com.panda.agent.conf.Configuration;
import com.panda.agent.connection.ClickhouseTemplate;
import com.panda.agent.disruptor.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LogEventHandlerManager {

    private static final Logger log = LoggerFactory.getLogger(LogEventHandlerManager.class);

    private static final LogEventHandlerManager instance = new LogEventHandlerManager();

    private LogEventHandlerManager() {
    }

    public static LogEventHandlerManager getInstance() {
        return instance;
    }

    /** 日志解析器 */
    private Map<String, LogEventParser> flagParserMap = new HashMap<>();
//    private LogEventConverter converter = new LogEventJsonConverter();

    private ClickhouseTemplate template;

    public void init() {
        String value = Configuration.getValue("log.parser.list");
        String[] array = value.split(",");
        if (array == null || array.length == 0) {
            return;
        }

        for (String parserName : array) {
            String messageFormat = Configuration.getValue("log.parser.message.format." + parserName);
            if (messageFormat == null || "".equals(messageFormat)) {
                continue;
            }

            register(new DefaultLogEventParser(parserName, messageFormat));
        }

        template = new ClickhouseTemplate();
        template.init();
    }

    private void register(LogEventParser parser) {
        flagParserMap.put(parser.getFlag(), parser);
    }

    public void handle(List<LogEvent> eventList) {
        Map<String, List<LogEvent>> flagEventListMap = new HashMap<>();
        for (LogEvent event : eventList) {
            String rawContent = event.getRawContent();
            int start = rawContent.indexOf("#");
            int end = rawContent.indexOf("#", start + 1);
            String flag = rawContent.substring(start + 1, end);

            List<LogEvent> logEvents = flagEventListMap.computeIfAbsent(flag, k -> new ArrayList<>(eventList.size()));
            logEvents.add(event);
        }

        for (Map.Entry<String, List<LogEvent>> entry : flagEventListMap.entrySet()) {
            String flag = entry.getKey();
            List<LogEvent> logEvents = entry.getValue();

            LogEventParser parser = flagParserMap.get(flag);
            if (parser == null) {
                log.error("日志解析器不存在,{}", flag);
                continue;
            }

            List<Map<String, Object>> batchList = new ArrayList<>();
            for (LogEvent event : logEvents) {
                Map<String, Object> params = parser.parse(event);
                if (params != null) {
                    params.putAll(event.getAttachMap());
                    batchList.add(params);
                }
            }
            template.addBatch(parser.getName(), batchList);
        }
    }

    public void handleOnEndOfFile() {
        // 读取完文件后，检查一下clickhouse的缓存日志，达到发送条件的需要发送
        template.checkAndBatchSend();
    }

}
