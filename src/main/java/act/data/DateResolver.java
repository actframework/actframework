package act.data;

import act.conf.AppConfig;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.StringValueResolver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Singleton
public class DateResolver extends StringValueResolver<Date> {
    static Logger logger = L.get(DateResolver.class);

    private DateFormat dateFormat;

    @Inject
    public DateResolver(AppConfig config) {
        String patten = config.dateTimeFormat();
        if (patten.contains("8601")) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        } else {
            dateFormat = new SimpleDateFormat(patten);
        }
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
