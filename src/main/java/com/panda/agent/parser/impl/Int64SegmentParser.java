package com.panda.agent.parser.impl;

import com.panda.agent.parser.SegmentParser;

public class Int64SegmentParser implements SegmentParser<Long> {

    @Override
    public Long parse(String content) {
        return Long.parseLong(content);
    }

}
