package act.data;

import act.conf.AppConfig;
import act.data.annotation.Pattern;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.$;
import org.osgl.util.AnnotationAware;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;
import org.osgl.util.ValueObject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JodaDateTimeCodec extends StringValueResolver<DateTime> implements ValueObject.Codec<DateTime> {

    private DateTimeFormatter dateFormat;

    public JodaDateTimeCodec(DateTimeFormatter dateFormat) {
        this.dateFormat = $.notNull(dateFormat);
        verify();
    }

    public JodaDateTimeCodec(String format) {
        this.dateFormat = DateTimeFormat.forPattern(format);
        verify();
    }

    @Inject
    public JodaDateTimeCodec(AppConfig config) {
        String patten = config.dateTimeFormat();
        if (patten.contains("8601") || patten.contains("iso")) {
            dateFormat = ISODateTimeFormat.dateTime();
        } else {
            dateFormat = DateTimeFormat.forPattern(patten);
        }
        verify();
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

    @Override
    public StringValueResolver<DateTime> amended(AnnotationAware beanSpec) {
        Pattern pattern = beanSpec.getAnnotation(Pattern.class);
        return null == pattern ? this : new JodaDateTimeCodec(pattern.value());
    }

    private void verify() {
        DateTime now = DateTime.now();
        String s = toString(now);
        if (!s.equals(toString(parse(s)))) {
            throw new IllegalArgumentException("Invalid date time pattern");
        }
    }
}
