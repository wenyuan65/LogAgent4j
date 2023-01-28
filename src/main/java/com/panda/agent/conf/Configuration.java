package com.panda.agent.conf;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Configuration {

    private static final String Config_File_Name = "config.properties";

    private static long lastModifiedTime = -1L;

    private static Properties properties = null;

    public static void init() throws IOException {
        reload();

        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.MINUTES.sleep(3);

                    reload();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }, "ConfigurationMonitor").start();
    }

    private static void reload() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(Config_File_Name);
        File file = new File(url.getFile());
        long lastModified = file.lastModified();
        if (lastModifiedTime == lastModified) {
            return;
        }

        Properties tempProp = new Properties();
        tempProp.load(new FileInputStream(file));

        lastModifiedTime = lastModified;
        properties = tempProp;
    }

    public static String getValue(String key) {
        return properties.getProperty(key);
    }

    public static String getValue(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getIntValue(String key) {
        String value = properties.getProperty(key);
        return Integer.parseInt(value);
    }

    public static int getIntValue(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || "".equals(value)) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

}
