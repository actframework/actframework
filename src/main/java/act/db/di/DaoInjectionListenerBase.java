package act.db.di;

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

import act.app.DbServiceManager;
import act.db.DB;
import act.inject.DependencyInjectionListener;
import act.util.*;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DaoInjectionListenerBase extends LogSupportedDestroyableBase implements DependencyInjectionListener {

    protected Logger logger = LogManager.get(DaoInjectionListenerBase.class);

    // Map type parameter array to (Model type, service ID) pair
    private Map<List<Type>, $.T2<Class, String>> svcIdCache = new HashMap<>();

    @Override
    protected void releaseResources() {
        svcIdCache.clear();
    }

    protected $.T2<Class, String> resolve(List<Type> typeParameters) {
        $.T2<Class, String> resolved = svcIdCache.get(typeParameters);
        if (null == resolved) {
            resolved = findSvcId(typeParameters);
            svcIdCache.put(typeParameters, resolved);
        }
        return resolved;
    }

    private $.T2<Class, String> findSvcId(List<Type> typeParameters) {
        // the EbeanDao<Long, User> and MorphiaDao<User> case
        Type modelType = (typeParameters.size() > 1) ? typeParameters.get(1) : typeParameters.get(0);
        DB db = AnnotationUtil.declaredAnnotation((Class)modelType, DB.class);
        return $.T2((Class) modelType, null == db ? DbServiceManager.DEFAULT : db.value());
    }

}
