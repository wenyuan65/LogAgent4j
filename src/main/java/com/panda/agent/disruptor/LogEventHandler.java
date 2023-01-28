package com.panda.agent.disruptor;

import com.lmax.disruptor.EventHandler;
import com.panda.agent.parser.LogEventHandlerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LogEventHandler implements EventHandler<LogEvent> {

    private static final Logger log = LoggerFactory.getLogger(LogEventHandler.class);

    private List<LogEvent> batchEventList = new ArrayList<>();

    @Override
    public void onEvent(LogEvent logEvent, long sequence, boolean endOfBatch) throws Exception {
//        log.info("sequence:{}, endOfBatch:{}", sequence, endOfBatch);
        batchEventList.add(logEvent);

        if (endOfBatch) {
            LogEventHandlerManager.getInstance().handle(batchEventList);
            batchEventList.clear();
        }
    }
}
