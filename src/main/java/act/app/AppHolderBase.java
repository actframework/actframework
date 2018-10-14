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

import act.Act;
import act.Destroyable;
import act.util.LogSupportedDestroyableBase;
import org.osgl.$;

public abstract class AppHolderBase<T extends AppHolderBase> extends LogSupportedDestroyableBase implements AppHolder<T>, Destroyable {

    private App app;

    protected AppHolderBase() {
        app = Act.app();
    }

    protected AppHolderBase(App app) {
        this.app = $.requireNotNull(app);
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
