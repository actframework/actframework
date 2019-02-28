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

import static act.Destroyable.Util.tryDestroyAll;

import act.Act;
import act.exception.AppStartTerminateException;
import act.exception.PortOccupiedException;
import act.internal.util.AppDescriptor;
import act.util.LogSupportedDestroyableBase;
import org.osgl.$;
import org.osgl.util.E;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

/**
 * Manage applications deployed on Act
 *
 * TODO: get rid of multi-tenant support
 */
public class AppManager extends LogSupportedDestroyableBase {

    private Map<Integer, App> byPort = new HashMap<>();

    private AppManager() {
    }

    @Override
    protected void releaseResources() {
        tryDestroyAll(byPort.values(), ApplicationScoped.class);
        byPort = null;
    }

    public AppManager loadSingleApp(AppDescriptor descriptor) {
        AppScanner.SINGLE_APP_SCANNER.scan(descriptor, _F.loadApp(this));
        return this;
    }

    public void deploy(App app) {
        load(app);
        refresh(app);
    }

    public void refresh() {
        Iterator<App> itr = appIterator();
        while (itr.hasNext()) {
            refresh(itr.next());
        }
    }

    public void refresh(App app) {
        app.refresh();
    }

    public void load(App app) {
        app.build();
        app.refresh();
        int port = app.config().httpPort();
        E.invalidConfigurationIf(port < 0, "Invalid http.port configuration: %s", port);
        loadIntoPortMap(port, app);
        app.hook();
    }

    public boolean unload(App app) {
        return unloadApp(app, byPort);
    }

    private boolean unloadApp(App app, Map<?, App> map) {
        for (Map.Entry<?, App> entry : map.entrySet()) {
            if (app == entry.getValue()) {
                app.destroy();
                map.remove(entry.getKey());
                return true;
            }
        }
        return false;
    }

    private void loadIntoPortMap(int port, App app) {
        App app0 = byPort.get(port);
        if (null != app0) {
            E.invalidConfigurationIf(!app.equals(app0), "Another application has already been deployed using port %s", port);
        } else {
            byPort.put(port, app);
        }
    }

    private Iterator<App> appIterator() {
        final Iterator<App> itrByPort = byPort.values().iterator();
        return new Iterator<App>() {
            boolean byPortFinished = !itrByPort.hasNext();

            @Override
            public boolean hasNext() {
                if (!byPortFinished) {
                    byPortFinished = !itrByPort.hasNext();
                }
                return !byPortFinished;
            }

            @Override
            public App next() {
                return byPortFinished ? null : itrByPort.next();
            }

            @Override
            public void remove() {
                E.unsupport();
            }
        };
    }

    public static AppManager create() {
        return new AppManager();
    }

    private enum _F {
        ;

        static final $.F1<App, ?> loadApp(final AppManager mgr) {
            return new $.Visitor<App>() {
                @Override
                public void visit(App app) throws $.Break {
                    try {
                        mgr.load(app);
                    } catch (PortOccupiedException e) {
                        Act.LOGGER.fatal("Cannot start ActFramework: %s", e.getMessage());
                        Act.shutdown(app);
                        throw new AppStartTerminateException();
                    } catch (RuntimeException e) {
                        Act.shutdown(app);
                        throw e;
                    }
                }
            };
        }
    }
}
