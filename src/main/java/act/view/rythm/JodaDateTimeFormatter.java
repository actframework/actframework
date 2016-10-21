package act.view.rythm;

import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.rythmengine.extension.IFormatter;

import java.util.Locale;

public class JodaDateTimeFormatter implements IFormatter {
    @Override
    public String format(Object o, String pattern, Locale locale, String s1) {
        if (o instanceof ReadableInstant) {
            return JodaTransformers.format((ReadableInstant) o, pattern);
        } else if (o instanceof ReadablePartial) {
            return JodaTransformers.format((ReadablePartial) o, pattern);
        }
        return null;
    }
}
