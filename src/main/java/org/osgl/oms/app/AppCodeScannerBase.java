package org.osgl.oms.app;

import org.osgl._;
import org.osgl.oms.conf.AppConfig;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Set;

/**
 * Base class for {@link AppCodeScanner} implementations
 */
public abstract class AppCodeScannerBase implements AppCodeScanner {
    private App app;

    @Override
    public final void setApp(App app) {
        E.NPE(app);
        E.illegalStateIf(null != this.app && this.app != app, "%s has already been stamped", this);
        this.app = app;
        onAppSet();
    }

    protected void onAppSet() {}

    @Override
    public final boolean start(String className) {
        if (!shouldScan(className)) {
            return false;
        }
        reset0();
        reset(className);
        return true;
    }

    protected final App app() {
        return app;
    }

    protected final AppConfig config() {
        return app().config();
    }

    protected abstract void reset0();

    protected void reset(String className) {}

    protected abstract boolean shouldScan(String className);

    @Override
    public int hashCode() {
        return _.hc(app, getClass());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AppCodeScannerBase) {
            AppCodeScannerBase that = (AppCodeScannerBase) obj;
            return (that.app == app && that.getClass() == getClass());
        }
        return false;
    }

    @Override
    public String toString() {
        return S.builder(getClass().getName()).append("[").append(app).append("]").toString();
    }
}
