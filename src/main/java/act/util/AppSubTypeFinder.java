package act.util;

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

import act.app.App;
import act.app.event.SysEventId;

/**
 * An `AppSubTypeFinder` extends {@link SubTypeFinder} with an method to
 * apply the finder when application started.
 */
@SuppressWarnings("unused")
public abstract class AppSubTypeFinder<T> extends SubTypeFinder<T> {

    public AppSubTypeFinder(Class<T> target) {
        super(target);
        registerSingleton();
    }

    public AppSubTypeFinder(Class<T> target, SysEventId bindingEvent) {
        super(target, bindingEvent);
        registerSingleton();
    }

    private void registerSingleton() {
        App.instance().registerSingleton(getClass(), this);
    }

    @SuppressWarnings("unused")
    public static class _AppSubTypeFinderFinder extends SubTypeFinder<AppSubTypeFinder> {

        public _AppSubTypeFinderFinder() {
            super(AppSubTypeFinder.class, SysEventId.SINGLETON_PROVISIONED);
        }

        @Override
        protected void found(final Class<? extends AppSubTypeFinder> target, App app) {
            AppSubTypeFinder finder = app.getInstance(target);
            finder.applyTo(app);
        }
    }
}
