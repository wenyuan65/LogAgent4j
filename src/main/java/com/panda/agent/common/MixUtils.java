package com.panda.agent.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MixUtils {

    public static List<String> walkFiles(String dir) {
        dir = dir.replaceAll("\\\\", "/");
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }

        return doWalkFiles(dir, dir, new ArrayList<>(64));
    }

    private static List<String> doWalkFiles(String baseDir, String currentDir, List<String> list) {
        File file = new File(currentDir);
        File[] files = file.listFiles();
        if (files == null) {
            return list;
        }

        for (File tmp : files) {
            String path = currentDir + "/" + tmp.getName();
            if (tmp.isFile()) {
//                path = path.substring(baseDir.length() + 1);
                list.add(path);
            } else {
                doWalkFiles(baseDir, path, list);
            }
        }

        return list;
    }

    public static boolean isSameDay(Date date1, Date date2) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date11 = cal.getTime();

        cal = Calendar.getInstance();
        cal.setTime(date2);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date21 = cal.getTime();

        return date11.getTime() == date21.getTime();
    }

    public static byte[] getBytes(long value) {
        byte[] result = new byte[8];
        result[0] = (byte)(value >> 56);
        result[1] = (byte)(value >> 48);
        result[2] = (byte)(value >> 40);
        result[3] = (byte)(value >> 32);
        result[4] = (byte)(value >> 24);
        result[5] = (byte)(value >> 16);
        result[6] = (byte)(value >> 8);
        result[7] = (byte)(value);

        return result;
    }

    public static long getLong(byte[] byteArray) {
        return ((((long)byteArray[0]       ) << 56) |
                (((long)byteArray[1] & 0xff) << 48) |
                (((long)byteArray[2] & 0xff) << 40) |
                (((long)byteArray[3] & 0xff) << 32) |
                (((long)byteArray[4] & 0xff) << 24) |
                (((long)byteArray[5] & 0xff) << 16) |
                (((long)byteArray[6] & 0xff) <<  8) |
                (((long)byteArray[7] & 0xff)      ));
    }

    public static void main(String[] args) {
        List<String> list = walkFiles("E:\\apps");
        for (String tmp : list) {
            System.out.println(tmp);
        }

        System.out.println(isSameDay(new Date(), new Date(System.currentTimeMillis() + 12300)));
        System.out.println(isSameDay(new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))));

        System.out.println(getLong(getBytes(8234567890987654321L)));
        System.out.println(getLong(getBytes(-8234567890987654321L)));
    }

}
