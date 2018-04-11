package act.inject;

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

import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.Generics;

import java.lang.reflect.Type;
import java.util.List;
import javax.inject.Provider;

/**
 * App implemented {@link javax.inject.Provider} can extends this base class to automatically register to injector
 *
 * **Note** this class will automatically register to ACT's injector. Thus if you need to configure
 */
public abstract class ActProvider<T> implements Provider<T> {

    private final Class<T> targetType;

    public ActProvider() {
        targetType = exploreTypeInfo();
    }

    protected ActProvider(Class<T> targetType) {
        this.targetType = $.requireNotNull(targetType);
    }

    public Class<T> targetType() {
        return targetType;
    }

    private Class<T> exploreTypeInfo() {
        List<Type> types = Generics.typeParamImplementations(getClass(), ActProvider.class);
        int sz = types.size();
        E.illegalStateIf(1 != sz, "generic type number not match");
        Type type = types.get(0);
        E.illegalArgumentIf(!(type instanceof Class), "generic type is not a class: %s", type);
        return (Class) type;
    }

}


