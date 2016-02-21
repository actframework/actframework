package act.db.util;

import act.db.TimestampGenerator;
import act.db.TimestampGeneratorBase;
import org.joda.time.DateTime;
import org.joda.time.Instant;

public class JodaInstantTsGenerator extends TimestampGeneratorBase<Instant> {
    @Override
    public Class<Instant> timestampType() {
        return Instant.class;
    }

    @Override
    public Instant now() {
        return Instant.now();
    }
}
