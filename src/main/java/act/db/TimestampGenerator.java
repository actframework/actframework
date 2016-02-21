package act.db;

public interface TimestampGenerator<TIMESTAMP_TYPE> {
    TIMESTAMP_TYPE now();
    Class<TIMESTAMP_TYPE> timestampType();
}
