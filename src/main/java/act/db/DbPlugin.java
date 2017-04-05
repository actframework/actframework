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
import act.plugin.Plugin;
import act.util.DestroyableBase;

import java.util.Map;

/**
 * The base class for Database Plugin
 */
public abstract class DbPlugin extends DestroyableBase implements Plugin {
    @Override
    public void register() {
        Act.dbManager().register(this);
        Act.trigger(new DbPluginRegistered(this));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || null != obj && getClass() == obj.getClass();
    }

    public abstract DbService initDbService(String id, App app, Map<String, String> conf);

    public void afterDbServiceLoaded() {
    }

}
