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

import act.asm.Type;
import org.osgl.$;

public class ClassMetaInfoBase<T extends ClassMetaInfoBase> {

    private String className;

    public T className(String className) {
        this.className = className;
        return me();
    }

    public T classInternalName(String classInternalName) {
        return className(Type.getObjectType(classInternalName).getClassName());
    }

    public T classDesc(String desc) {
        return className(Type.getType(desc).getClassName());
    }

    public String className() {
        return className;
    }

    protected final T me() {
        return $.cast(this);
    }

}
