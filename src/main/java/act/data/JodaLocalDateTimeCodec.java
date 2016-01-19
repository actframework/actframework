package act.data;

import act.conf.AppConfig;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.util.ValueObject;

public class JodaLocalDateTimeCodec extends StringValueResolver<LocalDateTime> implements ValueObject.Codec<LocalDateTime> {

    private DateTimeFormatter dateFormat;

    public JodaLocalDateTimeCodec(AppConfig config) {
        String patten = config.dateFormat();
        if (patten.contains("8601")) {
            dateFormat = ISODateTimeFormat.dateTimeNoMillis();
        } else {
            dateFormat = DateTimeFormat.forPattern(patten);
        }
    }

    @Override
    public LocalDateTime resolve(String value) {
        return null == value ? null : dateFormat.parseLocalDateTime(value);
    }

    @Override
    public Class<LocalDateTime> targetClass() {
        return LocalDateTime.class;
    }

    @Override
    public LocalDateTime parse(String s) {
        return resolve(s);
    }

    @Override
    public String toString(LocalDateTime localDateTime) {
        return dateFormat.print(localDateTime);
    }

    @Override
    public String toJSONString(LocalDateTime localDateTime) {
        return null;
    }
}
