package act.db;

import act.app.ActionContext;
import act.app.security.SecurityContext;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;

public abstract class DaoBase<ID_TYPE, MODEL_TYPE, QUERY_TYPE extends Dao.Query<MODEL_TYPE, QUERY_TYPE>>
        implements Dao<ID_TYPE, MODEL_TYPE, QUERY_TYPE> {

    private ActionContext appCtx;
    private SecurityContext secCtx;
    private boolean destroyed;


    @Override
    public void setAppContext(ActionContext context) {
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

    protected final ActionContext appContext() {
        return appCtx;
    }

    protected final SecurityContext securityContext() {
        return secCtx;
    }

}
