package act.util;

import act.conf.AppConfig;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.util.S;
import org.osgl.util.ValueObject;

import javax.inject.Inject;

public class JodaDateTimeCodec implements ValueObject.Codec<DateTime> {

    private AppConfig config;

    @Inject
    public JodaDateTimeCodec(AppConfig config) {
        this.config = config;
    }

    @Override
    public Class targetClass() {
        return DateTime.class;
    }

    @Override
    public DateTime parse(String s) {
        DateTimeFormatter dtf = DateTimeFormat.forPattern(dateTimeFormat());
        return dtf.parseDateTime(s);
    }

    @Override
    public String toString(DateTime o) {
        DateTimeFormatter dtf = DateTimeFormat.forPattern(dateTimeFormat());
        return dtf.print(o);
    }

    @Override
    public String toJSONString(DateTime o) {
        return S.builder("\"").append(toString(o)).append("\"").toString();
    }

    private String dateTimeFormat() {
        // TODO: support get datetime format from action context
        return config.dateTimeFormat();
    }

}
