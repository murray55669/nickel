package nickel.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Murray on 21/07/2017
 */
public class DateUtil {

    private static final Date START_DATE = new Date();
    private static String timestamp = null;

    public static String getTimestamp() {
        if (timestamp == null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
            timestamp = dateFormat.format(START_DATE);
        }
        return timestamp;
    }
}
