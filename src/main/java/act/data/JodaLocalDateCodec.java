package act.data;

import act.conf.AppConfig;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.util.StringValueResolver;
import org.osgl.util.ValueObject;

public class JodaLocalDateCodec extends StringValueResolver<LocalDate> implements ValueObject.Codec<LocalDate> {

    private DateTimeFormatter dateFormat;

    public JodaLocalDateCodec(AppConfig config) {
        String patten = config.dateFormat();
        if (patten.contains("8601")) {
            dateFormat = ISODateTimeFormat.date();
        } else {
            dateFormat = DateTimeFormat.forPattern(patten);
        }
    }

    @Override
    public LocalDate resolve(String value) {
        return null == value ? null : dateFormat.parseLocalDate(value);
    }

    @Override
    public Class<LocalDate> targetClass() {
        return LocalDate.class;
    }

    @Override
    public LocalDate parse(String s) {
        return resolve(s);
    }

    @Override
    public String toString(LocalDate localDate) {
        return dateFormat.print(localDate);
    }

    @Override
    public String toJSONString(LocalDate localDate) {
        return null;
    }
}
