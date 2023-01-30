package com.panda.agent;


import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.panda.agent.common.MixUtils;
import com.panda.agent.conf.Configuration;
import com.panda.agent.core.AgentManager;
import com.panda.agent.core.FileAgent;
import com.panda.agent.disruptor.LogEvent;
import com.panda.agent.disruptor.LogEventFactory;
import com.panda.agent.disruptor.LogEventHandler;
import com.panda.agent.parser.LogEventHandlerManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <pre>
 * logFile --> Agent --> Parser --> convert --> clickhouse
 * FileAgent: 监视日志文件变化，获取变化的日志内容
 * Parser: 解析日志内容
 * convert：将日志内容转换成接收平台需要的格式
 * clickhouse：日志接收平台的客户端实现接收消息
 * </pre>
 */
public class Main {

//    private static final Logger log = LoggerFactory.getLogger(FileAgent.class);

    public static void main(String[] args) {
        try {
            start();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void start() throws Throwable {
        Configuration.init();

        LogEventHandlerManager.getInstance().init();

        Disruptor<LogEvent> disruptor = initDisruptor();

        initFileAgents(disruptor);
    }

    private static Disruptor<LogEvent> initDisruptor() {
        int ringBufferSize = 8 * 1024;
        Disruptor<LogEvent> disruptor = new Disruptor<>(new LogEventFactory(), ringBufferSize, (ThreadFactory) r -> new Thread(r), ProducerType.SINGLE, new SleepingWaitStrategy());
        disruptor.handleEventsWith(new LogEventHandler());
        disruptor.start();

        return disruptor;
    }

    private static void initFileAgents(Disruptor<LogEvent> disruptor) {
        String game = Configuration.getValue("log.game");
        String baseDir = Configuration.getValue("base.dir");
        String fileRegex = Configuration.getValue("log.file.regex");
        Pattern pattern = Pattern.compile(fileRegex);

        // 初始化agent
        AgentManager.getInstance().init();

        Date now = new Date();
        List<String> filePathList = MixUtils.walkFiles(baseDir);
        List<String> historyLogFileCandidateList = new ArrayList<>(filePathList.size());
        List<FileAgent> fileAgentList = new ArrayList<>(filePathList.size());
        for (String filePath : filePathList) {
            Matcher matcher = pattern.matcher(filePath);
            if (!matcher.find()) {
                historyLogFileCandidateList.add(filePath);
                continue;
            }

            // 获取服务器名称
            String serverName = matcher.group(1);
            // 从服务器名称上解析游戏名称
            String agentGame = game;
            int index = serverName.indexOf("_");
            if (index > 0) {
                agentGame = serverName.substring(0, index);
            }

            FileAgent fileAgent = new FileAgent(disruptor.getRingBuffer());
            fileAgent.setFilePath(filePath);
            fileAgent.setPos(0);
            fileAgent.setServerName(serverName);
            fileAgent.setTimestamp(now);
            fileAgent.setGame(agentGame);
            fileAgentList.add(fileAgent);
        }

        // 处理历史日志文件
        for (FileAgent fileAgent : fileAgentList) {
//            Iterator<String> it = historyLogFileCandidateList.iterator();
//            while (it.hasNext()) {
//                String historyLogFilePath = it.next();
//                if (fileAgent.addHistoryLogFile(historyLogFilePath)) {
//                    it.remove();
//                }
//            }

            AgentManager.getInstance().addAgent(fileAgent);
        }

        AgentManager.getInstance().start();
    }

}
