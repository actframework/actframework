package act.app.util;

import act.db.DaoBase;
import act.db.DbBind;
import act.db.ModelBase;
import org.osgl.$;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

/**
 * A class template for simple RESTful service
 */
public abstract class
SimpleRestfulServiceBase<
        ID_TYPE,
        MODEL_TYPE extends ModelBase<ID_TYPE, MODEL_TYPE>,
        DAO_TYPE extends DaoBase<ID_TYPE, MODEL_TYPE, ?>> {

    private DAO_TYPE dao;

    public SimpleRestfulServiceBase(DAO_TYPE dao) {
        this.dao = $.notNull(dao);
    }

    @GetAction
    public Iterable<MODEL_TYPE> list() {
        return dao.findAll();
    }

    @GetAction("{id}")
    public MODEL_TYPE get(@DbBind("id") MODEL_TYPE model) {
        return model;
    }

    @PostAction
    public MODEL_TYPE create(MODEL_TYPE model) {
        return dao.save(model);
    }

    @DeleteAction("{id}")
    public void delete(ID_TYPE id) {
        dao.deleteById(id);
    }

}
