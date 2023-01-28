package com.panda.agent.naming;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DefaultNamingStrategy implements NamingStrategy {

    @Override
    public String getName(String filePath, Date date) {
        if (filePath.endsWith(".log")) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String format = sdf.format(date);

            return filePath.substring(0, filePath.length() - 4) + "_" + format + ".log";
        }

        return filePath;
    }

    @Override
    public boolean isHistoryLogFile(String filePath, String historyFilePath) {
        if (!filePath.endsWith(".log") || !historyFilePath.endsWith(".log")) {
            return false;
        }

        // 去除日期信息后，如果两个文件的其他部分相同，那么则是历史文件
        String filePath2 = filePath.substring(0, filePath.length() - 4);
        String historyFilePath2 = historyFilePath.substring(0, filePath.length() - 15);

        return filePath2.equalsIgnoreCase(historyFilePath2);
    }
}
