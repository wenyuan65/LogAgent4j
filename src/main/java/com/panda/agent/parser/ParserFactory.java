package com.panda.agent.parser;

import com.panda.agent.parser.impl.DateTimeSegmentParser;
import com.panda.agent.parser.impl.Int64SegmentParser;
import com.panda.agent.parser.impl.IntSegmentParser;
import com.panda.agent.parser.impl.StringSegmentParser;

public class ParserFactory {

    public static SegmentParser createSegment(String param) {
        String[] args = param.split(",");
        String segmentType = args[0];

        switch (segmentType) {
            case "string" : return new StringSegmentParser();
            case "date_time" : return new DateTimeSegmentParser(args[1]);
            case "int64" : return new Int64SegmentParser();
            case "int" : return new IntSegmentParser();
        }
        throw new RuntimeException("unknown segment type:" + segmentType);
    }

}
