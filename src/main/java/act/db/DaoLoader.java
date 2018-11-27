package act.db;

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
import act.app.App;
import act.app.DbServiceManager;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.GenericTypedBeanLoader;

import java.lang.reflect.Type;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DaoLoader implements GenericTypedBeanLoader<Dao> {

    private DbServiceManager dbServiceManager;
    public DaoLoader() {
        dbServiceManager = App.instance().dbServiceManager();
    }

    @Override
    public Dao load(BeanSpec spec) {
        List<Type> typeList = spec.typeParams();
        int sz = typeList.size();
        if (sz > 1) {
            Class<?> modelType = BeanSpec.rawTypeOf(typeList.get(1));
            return dbServiceManager.dao(modelType);
        }
        return (Dao)Act.getInstance(spec.rawType());
    }

}
