package act.app;

import act.Destroyable;
import act.util.DestroyableBase;
import org.osgl.util.E;

public abstract class AppHolderBase<T extends AppHolderBase> extends DestroyableBase implements AppHolder<T>, Destroyable {

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
