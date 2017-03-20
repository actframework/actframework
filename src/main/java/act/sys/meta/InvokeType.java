package act.sys.meta;

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

public enum InvokeType {
    STATIC () {
        @Override
        public <T> T newInstance(Class<T> cls, App app) {
            return null;
        }
    }, VIRTUAL () {
        @Override
        public <T> T newInstance(Class<T> cls, App app) {
            return app.getInstance(cls);
        }
    };
    public abstract <T> T newInstance(Class<T> cls, App app);
}
