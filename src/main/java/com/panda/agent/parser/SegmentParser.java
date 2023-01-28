package com.panda.agent.parser;

public interface SegmentParser<T> {

    T parse(String content);

}
