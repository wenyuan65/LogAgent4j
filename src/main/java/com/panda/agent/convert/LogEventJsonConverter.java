package com.panda.agent.convert;

import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

public class LogEventJsonConverter implements LogEventConverter {

    @Override
    public String convert(Map<String, Object> params) {
        return JSONObject.toJSONString(params);
    }

}
