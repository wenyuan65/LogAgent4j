import com.panda.agent.common.MixUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogFileContentAppender {

    private static final String path = "E:/apps/zjzr_xianyuandroid_2/logs/dayreport/dayreport.log";

    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String content = "%s#i#user12345#123456789#玩家%d#player@getPlayerInfo#{id:1,index:2}#12#xianyu#android#1.0.0.1#";

        Thread t1 = new Thread(() -> {
            System.out.println("线程1开始执行");
            try {
                FileWriter fw = new FileWriter(path, false);
                for (int j = 0; j < 100; j++) {
                    int n = 10000;
                    while (n > 0) {
                        String now = sdf.format(new Date());
                        String format = String.format(content, now, n);

                        fw.write(format);
                        fw.write(System.lineSeparator());
                        n--;
                    }
                    Thread.sleep(0);
                }
                fw.flush();
                fw.close();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            System.out.println("线程1执行完成");
        }, "write-Thread");
        Thread t2 = new Thread(() -> {
            System.out.println("线程2开始执行");
            int n = 0;
            try {
                RandomAccessFile raf = new RandomAccessFile(path, "r");
                while (true) {
                    String line = raf.readLine();
                    if (line == null) {
                        break;
                    }
                    n++;
                }

                System.out.println("读取数据行：" + n);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("线程2执行完成");
        }, "read-thread");

        try {
            t1.start();
            Thread.sleep(500);
            t2.start();

            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("结束");
    }

}
