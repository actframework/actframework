package act.db.util;

import act.db.TimestampGeneratorBase;
import org.joda.time.DateTime;
import org.osgl.$;

public class LongTsGenerator extends TimestampGeneratorBase<Long> {
    @Override
    public Class<Long> timestampType() {
        return Long.class;
    }

    @Override
    public Long now() {
        return $.ms();
    }
}
