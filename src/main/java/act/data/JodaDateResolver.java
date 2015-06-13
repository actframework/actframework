package act.data;

import act.conf.AppConfig;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.util.StringValueResolver;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JodaDateResolver extends StringValueResolver<DateTime> {
    static Logger logger = L.get(JodaDateResolver.class);

    private DateTimeFormatter dateFormat;

    public JodaDateResolver(AppConfig config) {
        dateFormat = DateTimeFormat.forPattern(config.dateFormat());
    }

    public JodaDateResolver(String pattern) {
        dateFormat = DateTimeFormat.forPattern(pattern);
    }

    @Override
    public DateTime resolve(String value) {
        return dateFormat.parseDateTime(value);
    }
}
