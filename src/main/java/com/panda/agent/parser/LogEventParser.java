package com.panda.agent.parser;

import com.panda.agent.disruptor.LogEvent;

import java.util.Map;

public interface LogEventParser {

    String getName();

    String getFlag();

    Map<String, Object> parse(LogEvent event);

}
