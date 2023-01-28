package com.panda.agent.core;

import com.panda.agent.conf.Configuration;
import com.panda.agent.parser.LogEventHandlerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentManager {

    private static final Logger log = LoggerFactory.getLogger(AgentManager.class);

    private static final AgentManager instance = new AgentManager();

    private AgentManager() {
    }

    public static AgentManager getInstance() {
        return instance;
    }

    private ConcurrentHashMap<String, FileAgent> agentMap = new ConcurrentHashMap<>();

    private Map<String, FileAgentCachedConfig> configMap = new HashMap<>();

    public void init() {
        String cachedFilePath = Configuration.getValue("log.progress.cache");
        if (cachedFilePath == null || "".equals(cachedFilePath)) {
            return;
        }

        File file = new File(cachedFilePath);
        if (!file.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            while (fis.available() > 0) {
                FileAgentCachedConfig config = FileAgent.readObject(fis);
                if (config != null) {
                    configMap.put(config.getFilePath(), config);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        String value = Configuration.getValue("log.parser.sleep.interval");
        final long interval = Long.parseLong(value);

        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    long start = System.currentTimeMillis();
                    update();
                    long costTime = System.currentTimeMillis() - start;

                    if (costTime < interval) {
//                        log.info("sleep {} ms", interval - costTime);
                        Thread.sleep(interval - costTime);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }, "monitor-thread");
        thread.start();
    }

    public void addAgent(FileAgent fileAgent) {
        FileAgentCachedConfig config = configMap.get(fileAgent.getFilePath());
        fileAgent.init(config);

        agentMap.put(fileAgent.getFilePath(), fileAgent);
    }

    public void remove(FileAgent fileAgent) {
        agentMap.remove(fileAgent.getFilePath());
    }

    public void update() {
        String value = Configuration.getValue("log.parser.limit");
        int limit = Integer.parseInt(value);

        boolean anyReachEndOfFile = false;
        for (Map.Entry<String, FileAgent> entry : agentMap.entrySet()) {
            FileAgent fileAgent = entry.getValue();

            boolean canReadNextTime = fileAgent.readFile(limit);
            if (!canReadNextTime) {
//                log.info("read end of file :{}", fileAgent.getFilePath());
                anyReachEndOfFile = true;
            }
        }

        if (anyReachEndOfFile) {
            LogEventHandlerManager.getInstance().handleOnEndOfFile();
        }

        saveFileAgentsProgress();
    }

    private void saveFileAgentsProgress() {
        String cachedFilePath = Configuration.getValue("log.progress.cache");
        if (cachedFilePath == null || "".equals(cachedFilePath)) {
            return;
        }

        File file = new File(cachedFilePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            for (Map.Entry<String, FileAgent> entry : agentMap.entrySet()) {
                FileAgent.writeObject(entry.getValue(), out);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
