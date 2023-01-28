package com.panda.agent.parser;

import com.panda.agent.conf.Configuration;
import com.panda.agent.disruptor.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DefaultLogEventParser implements LogEventParser {

    private static final Logger log = LoggerFactory.getLogger(LogEventParser.class);

    private String name;
    private String messageFormat;

    private String flag;
    private String[] segmentNames;
    private SegmentParser[] segmentParsers;

    public DefaultLogEventParser(String name, String messageFormat) {
        this.name = name;
        this.messageFormat = messageFormat;

        this.segmentNames = messageFormat.split("#");
        this.segmentParsers = new SegmentParser[segmentNames.length];
        for (int i = 0; i < segmentNames.length; i++) {
            if (i == 1) {
                this.flag = segmentNames[i];
                this.segmentNames[i] = "flag";
                this.segmentParsers[i] = ParserFactory.createSegment("string");
            } else {
                String param = Configuration.getValue("log.parser.message." + segmentNames[i]);
                this.segmentParsers[i] = ParserFactory.createSegment(param);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFlag() {
        return flag;
    }

    @Override
    public Map<String, Object> parse(LogEvent event) {
        Map<String, Object> map = new HashMap<>();

        String[] args = event.getRawContent().split("#");
        for (int i = 0; i < segmentNames.length; i++) {
            String segmentName = this.segmentNames[i];
            SegmentParser segmentParser = this.segmentParsers[i];
            Object object = segmentParser.parse(args[i]);

            map.put(segmentName, object);
        }

        return map;
    }

}
