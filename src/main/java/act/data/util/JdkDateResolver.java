package act.data.util;

import org.osgl.$;

import java.util.Date;

public class JdkDateResolver extends $.Transformer<Date, Long> {
    @Override
    public Long transform(Date date) {
        return null == date ? null : date.getTime();
    }
}
