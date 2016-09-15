package act.data;

import act.conf.AppConfig;
import act.data.annotation.Pattern;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.$;
import org.osgl.util.AnnotationAware;
import org.osgl.util.StringValueResolver;
import org.osgl.util.ValueObject;

import javax.inject.Inject;

public class JodaLocalDateTimeCodec extends StringValueResolver<LocalDateTime> implements ValueObject.Codec<LocalDateTime> {

    private DateTimeFormatter dateFormat;

    public JodaLocalDateTimeCodec(DateTimeFormatter dateFormat) {
        this.dateFormat = $.notNull(dateFormat);
        verify();
    }

    public JodaLocalDateTimeCodec(String pattern) {
        this.dateFormat = DateTimeFormat.forPattern(pattern);
        verify();
    }


    @Inject
    public JodaLocalDateTimeCodec(AppConfig config) {
        String patten = config.dateFormat();
        if (patten.contains("8601")) {
            dateFormat = ISODateTimeFormat.dateTimeNoMillis();
        } else {
            dateFormat = DateTimeFormat.forPattern(patten);
        }
        verify();
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


    @Override
    public StringValueResolver<LocalDateTime> amended(AnnotationAware beanSpec) {
        Pattern pattern = beanSpec.getAnnotation(Pattern.class);
        return null == pattern ? this : new JodaLocalDateTimeCodec(pattern.value());
    }

    private void verify() {
        LocalDateTime now = LocalDateTime.now();
        String s = toString(now);
        if (!s.equals(toString(parse(s)))) {
            throw new IllegalArgumentException("Invalid date time pattern");
        }
    }
}
