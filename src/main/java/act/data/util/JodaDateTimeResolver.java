package act.data.util;

import org.joda.time.DateTime;
import org.osgl.$;

public class JodaDateTimeResolver extends $.Transformer<DateTime, Long> {

    public static final JodaDateTimeResolver INSTANCE = new JodaDateTimeResolver();

    @Override
    public Long transform(DateTime dateTime) {
        return null == dateTime ? null : dateTime.getMillis();
    }
}
