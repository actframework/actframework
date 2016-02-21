package act.db.util;

import act.db.TimestampGeneratorBase;
import org.osgl.$;

import java.sql.Date;

public class SqlDateTsGenerator extends TimestampGeneratorBase<Date> {
    @Override
    public Class<Date> timestampType() {
        return Date.class;
    }

    @Override
    public Date now() {
        return new Date($.ms());
    }
}
