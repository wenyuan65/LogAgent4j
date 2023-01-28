package com.panda.agent.parser.impl;

import com.panda.agent.parser.SegmentParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeSegmentParser implements SegmentParser<Date> {

    private String format;

    public DateTimeSegmentParser(String format) {
        this.format = format;
    }

    @Override
    public Date parse(String content) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.parse(content);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

}
