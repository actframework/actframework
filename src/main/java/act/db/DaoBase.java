package act.db;

import act.app.security.SecurityContext;
import act.util.ActContext;
import org.osgl.util.Generics;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

public abstract class DaoBase<ID_TYPE, MODEL_TYPE, QUERY_TYPE extends Dao.Query<MODEL_TYPE, QUERY_TYPE>>
        implements Dao<ID_TYPE, MODEL_TYPE, QUERY_TYPE> {

    private ActContext appCtx;
    private SecurityContext secCtx;
    private boolean destroyed;
    protected Type modelType;
    protected Type idType;
    protected Type queryType;

    public DaoBase() {
        exploreTypes();
    }

    public DaoBase(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType) {
        this.idType = idType;
        this.modelType = modelType;
    }


    @Override
    public void setAppContext(ActContext context) {
        appCtx = context;
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        releaseResources();
        appCtx = null;
        secCtx = null;
    }

    @Override
    public Class<ID_TYPE> idType() {
        return Generics.classOf(idType);
    }

    @Override
    public Class<MODEL_TYPE> modelType() {
        return Generics.classOf(modelType);
    }

    @Override
    public Class<QUERY_TYPE> queryType() {
        return Generics.classOf(queryType);
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        secCtx = context;
    }

    protected void releaseResources() {}

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    protected final ActContext appContext() {
        return appCtx;
    }

    protected final SecurityContext securityContext() {
        return secCtx;
    }

    private void exploreTypes() {
        List<Type> types = Generics.typeParamImplementations(getClass(), DaoBase.class);
        int sz = types.size();
        if (sz < 1) {
            return;
        }
        if (sz > 2) {
            queryType = types.get(2);
        }
        if (sz > 1) {
            modelType = types.get(1);
        }
        idType = types.get(0);
    }

}
