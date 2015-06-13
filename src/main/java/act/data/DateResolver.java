package act.data;

import act.conf.AppConfig;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.util.StringValueResolver;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateResolver extends StringValueResolver<Date> {
    static Logger logger = L.get(DateResolver.class);

    private DateFormat dateFormat;

    public DateResolver(AppConfig config) {
        dateFormat = new SimpleDateFormat(config.dateFormat());
    }

    public DateResolver(String pattern) {
        dateFormat = new SimpleDateFormat(pattern);
    }

    @Override
    public Date resolve(String value) {
        try {
            return dateFormat.parse(value);
        } catch (ParseException e) {
            logger.error("error parsing date value from: %s", value);
            return null;
        }
    }
}
