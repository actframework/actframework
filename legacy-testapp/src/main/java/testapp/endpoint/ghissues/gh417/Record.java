package testapp.endpoint.ghissues.gh417;

import com.alibaba.fastjson.annotation.JSONField;
import org.joda.time.DateTime;

import java.util.Date;

public class Record {
    @JSONField(format = "yyyy-MMM-dd")
    public Date date1 = new Date();

    @JSONField(format = "yyyyMMdd")
    public Date date2 = date1;

    @JSONField(format = "yyyy_MM_dd hh:mm")
    public DateTime dateTime = DateTime.now();
}
