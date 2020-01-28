package act.db;

/**
 * a `DbSession` encapsulates information that is required
 * to interact with database, e.g.
 *
 * * DbService - provides db configuration/connection
 * * (nested) TxScope and associated Tx spec
 * * tenantId - for multi-tenancy support
 * * target table(s)/collection(s)
 * * entity (row/document) filter including pagination spec
 * * field/property (column) projector
 * * operation - CRUD
 * * mapper - map result back to model entity
 *
 */
public class DbSession {

    private DbService db;

    public DbService db() {
        return db;
    }

}
