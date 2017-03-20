package act.sys.meta;

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
import act.util.AsmTypes;

public class ReturnTypeInfo {
    private Type type;
    private Type componentType;

    public ReturnTypeInfo() {
        this(Type.VOID_TYPE);
    }

    private ReturnTypeInfo(Type type) {
        this.type = null == type ? Type.VOID_TYPE : type;
    }

    public Type type() {
        return type;
    }

    public ReturnTypeInfo componentType(Type type) {
        componentType = type;
        return this;
    }

    public Type componentType() {
        return componentType;
    }

    public boolean hasReturn() {
        return type != Type.VOID_TYPE;
    }

    public boolean isResult() {
        return AsmTypes.RESULT_TYPE.equals(type);
    }

    public static ReturnTypeInfo of(Type type) {
        return new ReturnTypeInfo(type);
    }
}
