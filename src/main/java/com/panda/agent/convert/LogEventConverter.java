package com.panda.agent.convert;

import java.util.Map;

public interface LogEventConverter {

    String convert(Map<String, Object> params);
}
