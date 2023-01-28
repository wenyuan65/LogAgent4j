package com.panda.agent.parser.impl;

import com.panda.agent.parser.SegmentParser;

public class IntSegmentParser implements SegmentParser<Integer> {

    @Override
    public Integer parse(String content) {
        return Integer.parseInt(content);
    }

}
