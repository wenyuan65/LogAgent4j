package com.panda.agent.core;

import com.lmax.disruptor.RingBuffer;
import com.panda.agent.common.MixUtils;
import com.panda.agent.disruptor.LogEvent;
import com.panda.agent.disruptor.LogEventTranslator;
import com.panda.agent.naming.DefaultNamingStrategy;
import com.panda.agent.naming.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FileAgent {

    private static final Logger log = LoggerFactory.getLogger(FileAgent.class);

    /** 文件路径 */
    private String filePath;
    /** 服务器名称 */
    private String serverName;
    /** 文件所属的游戏代号 */
    private String game;
    /** 记录已经读取的日志位置 */
    private long pos;
    /** 记录日志文件的长度 */
    private long len;
    /** 记录抓取的时间，防止跨天的时候，日志文件改名 */
    private Date timestamp;

    /** 历史日志文件 */
    private List<String> historyFilePathList = new ArrayList<>();
    /** 历史日志文件的命名规则 */
    private NamingStrategy namingStrategy = new DefaultNamingStrategy();

    private RingBuffer<LogEvent> ringBuffer;

    private int count = 0;

    private LogEventTranslator eventTranslator;

    public FileAgent(RingBuffer<LogEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void init(FileAgentCachedConfig config) {
        this.eventTranslator = new LogEventTranslator(game, serverName);
        // 历史日志文件排序
        Collections.sort(historyFilePathList);

        if (config != null) {
            this.pos = config.getPos();
            this.timestamp = new Date(config.getTime());

            // 存在历史读取记录的，将
            String oldFileName = namingStrategy.getName(filePath, timestamp);
        } else {
            // 没有历史记录，全部重新读

        }

        //TODO: 读取历史日志文件


        log.info("register file agent using cached config, path:{}, pos:{}, timestamp:{}, serverName:{}, game:{}", this.getFilePath(), this.getPos(), this.getTimestamp(), this.serverName, this.getGame());
    }

    public boolean addHistoryLogFile(String historyLogFilePath) {
        if (!namingStrategy.isHistoryLogFile(this.filePath, historyLogFilePath)) {
            return false;
        }
        historyFilePathList.add(historyLogFilePath);

        return true;
    }

    public boolean readFile(int limit) {
        Date now = new Date();
        if (MixUtils.isSameDay(now, timestamp)) {
            return doReadFile(filePath, limit, false);
        }

        // 日志在进入下一天的时候，可能重新命名，需要读取所有新剩余日志
        String oldFileName = namingStrategy.getName(filePath, timestamp);
        log.info("switching to next day, read leftover content for {} with modified file name: {}", filePath, oldFileName);

        boolean canReadNextTime = doReadFile(oldFileName, limit * 100, true);
        if (!canReadNextTime) {
            // 下一次不可以继续读的话，说明文件已经读取完了
            this.timestamp = now;
            this.pos = 0;

            log.info("switch to next day, reset data as new day, {}", filePath);
        }

        return canReadNextTime;
    }

    private boolean doReadFile(String filePath, int limit, boolean isLastDay) {
        boolean canReadNextTime = true;
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(filePath, "r");
            if (pos >= file.length()) {
                return false;
            }
            file.skipBytes((int)pos);

            for (int i = 0; i < limit; i++) {
                byte[] content = readLine(file);
                if (content == null || content.length == 0) {
                    canReadNextTime = false;
                    break;
                }

                String s = new String(content, StandardCharsets.UTF_8);
                ringBuffer.publishEvent(eventTranslator, s);
            }
            this.pos = file.getFilePointer();
            this.len = file.length();

            if (isLastDay || canPrint()) {
                log.info("read file {}, progress:{} / {}", filePath, this.pos, this.len);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                }
            }
        }

        return canReadNextTime;
    }

    private byte[] readLine(RandomAccessFile file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        int c = -1;
        boolean eol = false;
        while (!eol) {
            switch (c = file.read()) {
                case -1:
                case '\n':
                    eol = true;
                    break;
                case '\r':
                    eol = true;
                    long cur = file.getFilePointer();
                    if ((file.read()) != '\n') {
                        file.seek(cur);
                    }
                    break;
                default:
                    baos.write(c);
                    break;
            }
        }
        if ((c == -1) && (baos.size() == 0)) {
            return null;
        }

        return baos.toByteArray();
    }

    private boolean canPrint() {
        count ++;

        return count % 10000 == 0;
    }

    public static void writeObject(FileAgent agent, OutputStream out) throws IOException {
        byte[] bytes = agent.getFilePath().getBytes(StandardCharsets.UTF_8);
        out.write(bytes.length);
        out.write(bytes);
        out.write(MixUtils.getBytes(agent.getPos()));
        out.write(MixUtils.getBytes(agent.getTimestamp().getTime()));
    }

    public static FileAgentCachedConfig readObject(InputStream in) throws IOException {
        FileAgentCachedConfig config = new FileAgentCachedConfig();

        int len = in.read();
        byte[] filePathByteArray = new byte[len];
        byte[] posArray = new byte[8];
        byte[] timeArray = new byte[8];

        in.read(filePathByteArray);
        in.read(posArray);
        in.read(timeArray);

        config.setFilePath(new String(filePathByteArray, StandardCharsets.UTF_8));
        config.setPos(MixUtils.getLong(posArray));
        config.setTime(MixUtils.getLong(timeArray));

        return config;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public long getPos() {
        return pos;
    }

    public void setPos(long pos) {
        this.pos = pos;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
