package act.test.func;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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
import act.db.meta.EntityClassMetaInfo;
import act.db.meta.EntityFieldMetaInfo;
import act.db.meta.EntityMetaInfoRepo;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.N;
import org.osgl.util.S;

public class EntityId extends Func {

    private Class idFieldClass;
    private boolean isPrimitive;

    @Override
    public void init(Object param) {
        Class type = null;
        try {
            type = Act.classForName(S.string(param));
        } catch (Exception e) {
            throw new IllegalArgumentException("Class name expected. Found: " + param);
        }
        App app = Act.app();
        EntityFieldMetaInfo fieldMetaInfo = null;
        for (EntityMetaInfoRepo repo : app.entityMetaInfoRepo().allRepos()) {
            EntityClassMetaInfo metaInfo = repo.classMetaInfo(type);
            if (null != metaInfo) {
                fieldMetaInfo = metaInfo.idField();
            }
        }
        E.illegalArgumentIf(null == fieldMetaInfo, "Cannot find ID field of class: " + type);
        String fieldName = fieldMetaInfo.fieldName();
        idFieldClass = $.fieldOf(type, fieldName).getType();
        isPrimitive = $.isPrimitiveType(idFieldClass);
    }

    @Override
    public Object apply() {
        if (isPrimitive) {
            if (idFieldClass == int.class) {
                return N.randInt();
            } else if (idFieldClass == char.class) {
                return N.randInt(30, 50);
            } else if (idFieldClass == short.class) {
                return N.randInt(0, 100);
            } else if (idFieldClass == byte.class) {
                return N.randInt(0, 60);
            } else if (idFieldClass == long.class) {
                return N.randLong();
            } else if (idFieldClass == boolean.class) {
                return $.random(true, false);
            } else if (idFieldClass == float.class) {
                return N.randFloat();
            } else if (idFieldClass == double.class) {
                return N.randDouble();
            }
            return N.randInt();
        }
        return Act.getInstance(idFieldClass);
    }

}
