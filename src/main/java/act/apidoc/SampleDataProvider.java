package act.apidoc;

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

import act.util.LogSupport;
import org.osgl.util.Generics;

import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.List;

public abstract class SampleDataProvider<T> extends LogSupport implements Provider<T> {

    private Class<T> targetType;

    public SampleDataProvider() {
        exploreType();
    }

    /**
     * Return the sample data
     * @return the sample data
     */
    public abstract T get();

    public Class<T> targetType() {
        return targetType;
    }

    public ISampleDataCategory category() {
        return null;
    }

    private void exploreType() {
        List<Type> types = Generics.typeParamImplementations(getClass(), SampleDataProvider.class);
        if (types.size() != 1) {
            warnTargetTypeNotDetermined();
        } else {
            Type type = types.get(0);
            if (! (type instanceof Class)) {
                warnTargetTypeNotDetermined();
            } else {
                targetType = (Class<T>) type;
            }
        }
    }

    private void warnTargetTypeNotDetermined() {
        warn("Cannot determine target type of SampleDataProvider: " + getClass());
    }
}
