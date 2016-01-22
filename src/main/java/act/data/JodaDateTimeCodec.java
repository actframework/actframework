package act.data;

import act.conf.AppConfig;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.util.S;
import org.osgl.util.ValueObject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JodaDateTimeCodec extends StringValueResolver<DateTime> implements ValueObject.Codec<DateTime> {

    private DateTimeFormatter dateFormat;

    @Inject
    public JodaDateTimeCodec(AppConfig config) {
        String patten = config.dateTimeFormat();
        if (patten.contains("8601")) {
            dateFormat = ISODateTimeFormat.dateTime();
        } else {
            dateFormat = DateTimeFormat.forPattern(patten);
        }
    }

    @Override
    public DateTime resolve(String value) {
        return null == value ? null : dateFormat.parseDateTime(value);
    }

    @Override
    public Class targetClass() {
        return DateTime.class;
    }

    @Override
    public DateTime parse(String s) {
        return resolve(s);
    }

    @Override
    public String toString(DateTime o) {
        return dateFormat.print(o);
    }

    @Override
    public String toJSONString(DateTime o) {
        return S.builder("\"").append(toString(o)).append("\"").toString();
    }

}
