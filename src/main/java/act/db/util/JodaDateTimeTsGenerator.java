package act.db.util;

import act.db.TimestampGeneratorBase;
import org.joda.time.DateTime;

public class JodaDateTimeTsGenerator extends TimestampGeneratorBase<DateTime> {
    @Override
    public Class<DateTime> timestampType() {
        return DateTime.class;
    }

    @Override
    public DateTime now() {
        return DateTime.now();
    }
}
