package act.db.util;

import act.db.TimestampGenerator;
import act.db.TimestampGeneratorBase;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public class JodaLocalDateTimeTsGenerator extends TimestampGeneratorBase<LocalDateTime> {
    @Override
    public Class<LocalDateTime> timestampType() {
        return LocalDateTime.class;
    }

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
