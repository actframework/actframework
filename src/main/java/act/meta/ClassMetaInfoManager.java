package act.meta;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.plugin.AppServicePlugin;
import org.osgl.$;
import org.osgl.util.Generics;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ClassMetaInfoManager<INFO extends ClassMetaInfoBase<INFO>> extends AppServicePlugin {

    private Class<INFO> infoType;

    private Map<String, INFO> map = new HashMap<>();

    public ClassMetaInfoManager() {
        exploreTypes();
    }

    public Class<INFO> infoType() {
        return infoType;
    }

    @Override
    protected void applyTo(App app) {
        app.register(this);
        map.clear();
    }

    public INFO getOrCreate(String className) {
        INFO info = map.get(className);
        if (null == info) {
            info = $.newInstance(infoType);
            map.put(className, info);
        }
        return info;
    }

    public INFO get(String className) {
        return map.get(className);
    }

    private void exploreTypes() {
        List<Type> types = Generics.typeParamImplementations(getClass(), ClassMetaInfoManager.class);
        infoType = Generics.classOf(types.get(0));
    }
}
