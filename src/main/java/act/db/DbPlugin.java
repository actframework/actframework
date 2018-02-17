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
import act.db.meta.MasterEntityMetaInfoRepo;
import act.plugin.AppServicePlugin;
import act.plugin.Plugin;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

/**
 * The base class for Database Plugin
 */
public abstract class DbPlugin extends AppServicePlugin implements Plugin {

    @Override
    public void register() {
        super.register();
        Act.dbManager().register(this);
        Act.trigger(new DbPluginRegistered(this));
    }

    /**
     * Sub class to override this method to return a set
     * of annotation classes that marks on an entity(model) class
     * @return entity class annotations
     */
    public Set<Class<? extends Annotation>> entityAnnotations() {
        return C.set();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    protected void applyTo(App app) {
        Set<Class<? extends Annotation>> entityAnnotations = entityAnnotations();
        if (!entityAnnotations.isEmpty()) {
            MasterEntityMetaInfoRepo repo = app.entityMetaInfoRepo();
            for (Class<? extends Annotation> annoType : entityAnnotations) {
                repo.registerEntityAnnotation(annoType);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || null != obj && getClass() == obj.getClass();
    }

    public abstract DbService initDbService(String id, App app, Map<String, String> conf);

}
