package act.db.util;

import act.db.TimestampGenerator;
import act.db.TimestampGeneratorBase;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class JodaLocalDateTsGenerator extends TimestampGeneratorBase<LocalDate> {
    @Override
    public Class<LocalDate> timestampType() {
        return LocalDate.class;
    }

    @Override
    public LocalDate now() {
        return LocalDate.now();
    }
}
