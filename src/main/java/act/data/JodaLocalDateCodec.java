package act.data;

import act.conf.AppConfig;
import act.data.annotation.Pattern;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.$;
import org.osgl.util.AnnotationAware;
import org.osgl.util.StringValueResolver;
import org.osgl.util.ValueObject;

import javax.inject.Inject;

public class JodaLocalDateCodec extends JodaDateTimeCodecBase<LocalDate> {

    private DateTimeFormatter dateFormat;

    public JodaLocalDateCodec(DateTimeFormatter dateFormat) {
        this.dateFormat = $.notNull(dateFormat);
        verify();
    }

    public JodaLocalDateCodec(String pattern) {
        if (isIsoStandard(pattern)) {
            dateFormat = ISODateTimeFormat.date();
        } else {
            dateFormat = DateTimeFormat.forPattern(pattern);
        }
        verify();
    }

    @Inject
    public JodaLocalDateCodec(AppConfig config) {
        this(config.dateFormat());
    }

    @Override
    public LocalDate resolve(String value) {
        return null == value ? null : dateFormat.parseLocalDate(value);
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

    @Override
    public StringValueResolver<LocalDate> amended(AnnotationAware beanSpec) {
        Pattern pattern = beanSpec.getAnnotation(Pattern.class);
        return null == pattern ? this : new JodaLocalDateCodec(pattern.value());
    }

    private void verify() {
        LocalDate now = LocalDate.now();
        String s = toString(now);
        if (!s.equals(toString(parse(s)))) {
            throw new IllegalArgumentException("Invalid date time pattern");
        }
    }
}
