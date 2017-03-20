package act.app;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.conf.AppConfig;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Base class for {@link AppCodeScanner} implementations
 */
public abstract class AppCodeScannerBase implements AppCodeScanner {

    protected static Logger logger = L.get(AppCodeScanner.class);

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
        reset();
        reset(className);
        return true;
    }

    protected final App app() {
        return app;
    }

    protected final AppConfig config() {
        return app().config();
    }

    protected abstract void reset();

    protected void reset(String className) {}

    protected abstract boolean shouldScan(String className);

    @Override
    public int hashCode() {
        return $.hc(app, getClass());
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
        return S.concat(getClass().getName(), "[", app.toString(), "]");
    }
}
