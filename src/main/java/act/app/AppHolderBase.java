package act.app;

import act.Destroyable;
import act.util.DestroyableBase;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;

public abstract class AppHolderBase<T extends AppHolderBase> extends DestroyableBase implements AppHolder<T>, Destroyable {

    protected static final Logger logger = L.get(App.class);

    private App app;

    protected AppHolderBase() {
    }

    protected AppHolderBase(App app) {
        E.NPE(app);
        this.app = app;
    }

    public T app(App app) {
        this.app = app;
        return me();
    }

    public App app() {
        return app;
    }

    protected T me() {
        return (T) this;
    }

    @Override
    protected void releaseResources() {
        app = null;
    }

}
