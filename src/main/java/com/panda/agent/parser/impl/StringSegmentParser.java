package com.panda.agent.parser.impl;

import com.panda.agent.parser.SegmentParser;

public class StringSegmentParser implements SegmentParser<String> {

    @Override
    public String parse(String content) {
        return content;
    }

}
