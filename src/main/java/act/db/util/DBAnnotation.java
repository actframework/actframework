package act.db.util;

import act.db.DB;

import java.io.Serializable;
import java.lang.annotation.Annotation;

// Follow the implementation of com.google.inject.name.NamedImpl
public class DBAnnotation implements DB, Serializable {

    private String db;

    public DBAnnotation(String db) {
        this.db = db;
    }

    @Override
    public String value() {
        return db;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return DB.class;
    }

    @Override
    public int hashCode() {
        // This is specified in java.lang.Annotation.
        return (127 * "value".hashCode()) ^ db.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DB)) {
            return false;
        }

        DB other = (DB) o;
        return db.equals(other.value());
    }

    @Override
    public String toString() {
        return "@" + DB.class.getName() + "(value=" + db + ")";
    }

    private static final long serialVersionUID = 0;
}
