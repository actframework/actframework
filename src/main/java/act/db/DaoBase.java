package act.db;

import act.app.AppContext;
import act.app.security.SecurityContext;

public abstract class DaoBase<ID_TYPE, MODEL_TYPE, QUERY_TYPE extends Dao.Query<MODEL_TYPE, QUERY_TYPE>>
        implements Dao<ID_TYPE, MODEL_TYPE, QUERY_TYPE> {

    private AppContext appCtx;
    private SecurityContext secCtx;
    private boolean destroyed;

    @Override
    public void setAppContext(AppContext context) {
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

    protected final AppContext appContext() {
        return appCtx;
    }

    protected final SecurityContext securityContext() {
        return secCtx;
    }
}
