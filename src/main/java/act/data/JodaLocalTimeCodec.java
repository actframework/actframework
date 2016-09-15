package act.data;

import act.conf.AppConfig;
import act.data.annotation.Pattern;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.$;
import org.osgl.util.AnnotationAware;
import org.osgl.util.StringValueResolver;
import org.osgl.util.ValueObject;

import javax.inject.Inject;

public class JodaLocalTimeCodec extends StringValueResolver<LocalTime> implements ValueObject.Codec<LocalTime> {

    private DateTimeFormatter dateFormat;

    public JodaLocalTimeCodec(DateTimeFormatter dateFormat) {
        this.dateFormat = $.notNull(dateFormat);
        verify();
    }

    public JodaLocalTimeCodec(String pattern) {
        this.dateFormat = DateTimeFormat.forPattern(pattern);
        verify();
    }


    @Inject
    public JodaLocalTimeCodec(AppConfig config) {
        String patten = config.timeFormat();
        if (patten.contains("8601")) {
            dateFormat = ISODateTimeFormat.time();
        } else {
            dateFormat = DateTimeFormat.forPattern(patten);
        }
    }

    @Override
    public LocalTime resolve(String value) {
        return null == value ? null : dateFormat.parseLocalTime(value);
    }

    @Override
    public Class<LocalTime> targetClass() {
        return LocalTime.class;
    }

    @Override
    public LocalTime parse(String s) {
        return resolve(s);
    }

    @Override
    public String toString(LocalTime localTime) {
        return dateFormat.print(localTime);
    }

    @Override
    public String toJSONString(LocalTime localTime) {
        return null;
    }


    @Override
    public StringValueResolver<LocalTime> amended(AnnotationAware beanSpec) {
        Pattern pattern = beanSpec.getAnnotation(Pattern.class);
        return null == pattern ? this : new JodaLocalTimeCodec(pattern.value());
    }

    private void verify() {
        LocalTime now = LocalTime.now();
        String s = toString(now);
        if (!s.equals(toString(parse(s)))) {
            throw new IllegalArgumentException("Invalid date time pattern");
        }
    }
}
