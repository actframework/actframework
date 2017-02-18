package act.data;

import act.conf.AppConfig;
import act.data.annotation.Pattern;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.$;
import org.osgl.util.AnnotationAware;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JodaLocalTimeCodec extends JodaDateTimeCodecBase<LocalTime> {

    private DateTimeFormatter dateFormat;
    private boolean isIso;

    public JodaLocalTimeCodec(DateTimeFormatter dateFormat) {
        this.dateFormat = $.notNull(dateFormat);
        verify();
    }

    public JodaLocalTimeCodec(String pattern) {
        if (isIsoStandard(pattern)) {
            dateFormat = ISODateTimeFormat.timeNoMillis();
            isIso = true;
        } else {
            dateFormat = DateTimeFormat.forPattern(pattern);
        }
        verify();
    }

    @Inject
    public JodaLocalTimeCodec(AppConfig config) {
        this(config.timeFormat());
    }

    @Override
    public LocalTime resolve(String value) {
        if (S.notBlank(value)) {
            // See http://stackoverflow.com/questions/15642053/joda-time-parsing-string-throws-java-lang-illegalargumentexception/15642797#15642797
            if (isIso && !value.contains("Z")) {
                value += "Z";
            }
            return dateFormat.parseLocalTime(value);
        }
        return null;
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
