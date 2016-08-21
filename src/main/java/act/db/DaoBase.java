package act.db;

import act.app.security.SecurityContext;
import act.util.ActContext;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;

public abstract class DaoBase<ID_TYPE, MODEL_TYPE, QUERY_TYPE extends Dao.Query<MODEL_TYPE, QUERY_TYPE>>
        implements Dao<ID_TYPE, MODEL_TYPE, QUERY_TYPE> {

    private ActContext appCtx;
    private SecurityContext secCtx;
    private boolean destroyed;
    protected Class<MODEL_TYPE> modelType;
    protected Class<ID_TYPE> idType;

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
        return idType;
    }

    @Override
    public Class<MODEL_TYPE> modelType() {
        return modelType;
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

}
