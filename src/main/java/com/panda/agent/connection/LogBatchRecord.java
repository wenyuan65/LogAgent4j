package com.panda.agent.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogBatchRecord {

    private long lastBatchExecuteTime;
    private List<Map<String, Object>> batchList = new ArrayList<>();

    public long getLastBatchExecuteTime() {
        return lastBatchExecuteTime;
    }

    public void setLastBatchExecuteTime(long lastBatchExecuteTime) {
        this.lastBatchExecuteTime = lastBatchExecuteTime;
    }

    public List<Map<String, Object>> getBatchList() {
        return batchList;
    }

    public void setBatchList(List<Map<String, Object>> batchList) {
        this.batchList = batchList;
    }
}
