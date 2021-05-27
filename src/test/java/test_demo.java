import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import java.util.Date;

public class test_demo {

    @Test
    public void test() {
        DateTime now = DateTime.now();
        System.out.println();
        System.out.println(now.toString("yyyy-MM-dd hh:mm:ss"));
        System.out.println(new DateTime(now.year().get(), now.monthOfYear().get(), now.dayOfMonth().get(), 23, 59, 59).toString("yyyy-MM-dd hh:mm:ss"));
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
        System.out.println(DateTime.parse("2021-05-15 01:00", formatter).toString());
        System.out.println(now.compareTo(DateTime.parse("2021-05-14 18:00", formatter)));
    }

}
