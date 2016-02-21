package act.db.util;

import act.db.TimestampGeneratorBase;
import org.joda.time.DateTime;

import java.util.Date;

public class JavaDateTsGenerator extends TimestampGeneratorBase<Date> {
    @Override
    public Class<Date> timestampType() {
        return Date.class;
    }

    @Override
    public Date now() {
        return new Date();
    }
}
