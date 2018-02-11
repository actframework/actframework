package act.app;

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

import act.meta.ClassMetaInfoBase;
import act.meta.ClassMetaInfoManager;
import org.osgl.$;

import java.util.HashMap;
import java.util.Map;

public class ClassMetaInfoRepo extends AppServiceBase<ClassMetaInfoRepo> {

    // (meta type name -> (class name - > meta info object))
    Map<Class<? extends ClassMetaInfoBase>, ClassMetaInfoManager<?>> repo = new HashMap<>();

    protected ClassMetaInfoRepo(App app) {
        super(app);
    }

    @Override
    protected void releaseResources() {
        repo.clear();
    }

    public <T extends ClassMetaInfoBase<T>> ClassMetaInfoManager<T> manager(Class<T> metaInfoType) {
        return $.cast(repo.get(metaInfoType));
    }

    void register(ClassMetaInfoManager<?> classMetaInfoManager) {
        repo.put(classMetaInfoManager.infoType(), classMetaInfoManager);
    }


}
