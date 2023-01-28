import com.panda.agent.common.MixUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogFileContentAppender {

    private static final String path = "E:/apps/zjzr_xianyuandroid_1/logs/dayreport/dayreport.log";

    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String content = "%s#i#user12345#123456789#玩家%d#player@getPlayerInfo#{id:1,index:2}#12#xianyu#android#1.0.0.1#";
        for (int j = 0; j < 100; j++) {
            try {
                FileWriter fw = new FileWriter(path, true);
                int n = 10000;
                while (n > 0) {
                    String now = sdf.format(new Date());
                    String format = String.format(content, now, n);

                    fw.write(format);
                    fw.write(System.lineSeparator());
                    n --;
                }
                fw.flush();
                fw.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
