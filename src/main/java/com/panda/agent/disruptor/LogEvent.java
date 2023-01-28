package com.panda.agent.disruptor;

import java.util.HashMap;
import java.util.Map;

public class LogEvent {

    private String rawContent;

    private Map<String, Object> attachMap = new HashMap<>();

    public LogEvent() {
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public void put(String key, Object value) {
        attachMap.put(key, value);
    }

    public Map<String, Object> getAttachMap() {
        return attachMap;
    }
}
