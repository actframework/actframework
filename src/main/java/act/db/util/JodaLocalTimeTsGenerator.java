package act.db.util;

import act.db.TimestampGeneratorBase;
import org.joda.time.LocalTime;

public class JodaLocalTimeTsGenerator extends TimestampGeneratorBase<LocalTime> {
    @Override
    public Class<LocalTime> timestampType() {
        return LocalTime.class;
    }

    @Override
    public LocalTime now() {
        return LocalTime.now();
    }
}
