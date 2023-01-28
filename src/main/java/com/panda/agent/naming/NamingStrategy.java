package com.panda.agent.naming;

import java.util.Date;

public interface NamingStrategy {

    /**
     * 获取指定日期的历史文件名
     * @param filePath
     * @param date
     * @return
     */
    String getName(String filePath, Date date);

    /**
     * 根据日志文件的命名规则，判断文件是否是历史日志文件
     * @param filePath
     * @param historyFilePath
     * @return
     */
    boolean isHistoryLogFile(String filePath, String historyFilePath);

}
