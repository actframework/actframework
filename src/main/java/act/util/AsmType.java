package act.util;

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

import act.asm.Type;
import org.osgl.$;
import org.osgl.util.E;

public class AsmType<T> {
    private Class<T> cls;
    private Type type;

    public AsmType(Class<T> cls) {
        E.NPE(cls);
        this.cls = cls;
        this.type = Type.getType(cls);
    }

    public Type asmType() {
        return type;
    }

    public String className() {
        return cls.getName();
    }

    public String internalName() {
        return type.getInternalName();
    }

    public String desc() {
        return type.getDescriptor();
    }

    public static String classNameForDesc(String description) {
        return Type.getType(description).getClassName();
    }

    public static <T> Class<T> classForDesc(String desc) {
        Type type = Type.getType(desc);
        return classForType(type);
    }

    public static <T> Class<T> classForType(Type type) {
        String className = type.getClassName();
        return $.classForName(className, AsmType.class.getClassLoader());
    }

    public static String classNameForInternalName(String internalName) {
        return Type.getObjectType(internalName).getClassName();
    }

    public static Class<?> classForInternalName(String internalName) {
        return classForType(Type.getObjectType(internalName));
    }

    public static Class<?>[] methodArgumentTypesForDesc(String methodDesc) {
        Type[] types = Type.getArgumentTypes(methodDesc);
        int len = types.length;
        Class<?>[] argumentTypes = new Class[len];
        for (int i = 0; i < types.length; ++i) {
            argumentTypes[i] = classForType(types[i]);
        }
        return argumentTypes;
    }

}
