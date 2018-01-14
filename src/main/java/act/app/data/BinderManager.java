package act.app.data;

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
import act.app.AppServiceBase;
import act.conf.AppConfig;
import act.controller.meta.HandlerParamMetaInfo;
import act.data.FileBinder;
import act.data.SObjectBinder;
import org.osgl.mvc.util.Binder;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BinderManager extends AppServiceBase<BinderManager> {

    private Map<Object, Binder> binders = new HashMap<>();

    public BinderManager(App app) {
        super(app);
        registerBuiltInBinders(app.config());
    }

    @Override
    protected void releaseResources() {
        binders.clear();
    }

    public <T> BinderManager register(Class<T> targetType, Binder<T> binder) {
        binders.put(targetType, binder);
        return this;
    }

    public BinderManager register(HandlerParamMetaInfo paramMetaInfo, Binder binder) {
        binders.put(paramMetaInfo, binder);
        return this;
    }

    public Binder binder(Class<?> clazz) {
        return binders.get(clazz);
    }

    public Binder binder(HandlerParamMetaInfo paramMetaInfo) {
        return binders.get(paramMetaInfo);
    }

    private void registerBuiltInBinders(AppConfig config) {
        binders.put(File.class, new FileBinder());
        binders.put(ISObject.class, new SObjectBinder());
        binders.put(SObject.class, new SObjectBinder());
    }

}
