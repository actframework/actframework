package testapp.endpoint.ghissues.gh417;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.joda.time.DateTime;

import java.lang.reflect.Type;
import java.util.Date;

public class FastJsonIssue {
    public static class Record {
        @JSONField(format = "yyyy-MMM-dd")
        public Date date1 = new Date();

        @JSONField(format = "yyyyMMdd")
        public Date date2 = date1;

        @JSONField(format = "yyyy_MM_dd hh:mm")
        public DateTime dateTime = DateTime.now();
    }

public static class JodaDateTimeSerializer implements ObjectSerializer {
    @Override
    public void write(
            JSONSerializer serializer,
            Object object,
            Object fieldName,
            Type fieldType,
            int features
    ) {
        SerializeWriter out = serializer.getWriter();
        if (object == null) {
            out.writeNull();
        } else {
            Class cls = object.getClass();
            if (cls == DateTime.class) {
                out.writeString(object.toString());
            } else {
                out.writeString(object.toString());
            }
        }
    }
}

    public static void main(String[] args) {
        SerializeConfig.getGlobalInstance().put(DateTime.class, new JodaDateTimeSerializer());
        System.out.println(JSON.toJSONString(new Record(), true));
    }
}
