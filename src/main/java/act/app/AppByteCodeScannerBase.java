package act.app;

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

import org.osgl.util.C;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all {@link AppByteCodeScanner} implementations
 */
public abstract class AppByteCodeScannerBase extends AppCodeScannerBase implements AppByteCodeScanner {
    
    private Map<Class<? extends AppByteCodeScanner>, Set<String>> dependencyClasses;

    protected final void reset() {
        dependencyClasses = new HashMap<>();
    }

    protected final void addDependencyClass(String className) {
        Set<String> set = dependencyClasses.get(getClass());
        if (null == set) {
            set = C.newSet();
            dependencyClasses.put(getClass(), set);
        }
        set.add(className);
    }

    protected final void addDependencyClassToScanner(Class<? extends AppByteCodeScanner> scannerClass, String className) {
        Set<String> set = dependencyClasses.get(scannerClass);
        if (null == set) {
            set = C.newSet();
            dependencyClasses.put(scannerClass, set);
        }
        set.add(className);
    }

    protected final void addDependencyClassToScanner(Class<? extends AppByteCodeScanner> scannerClass, Collection<String> classNames) {
        Set<String> set = dependencyClasses.get(scannerClass);
        if (null == set) {
            set = C.newSet();
            dependencyClasses.put(scannerClass, set);
        }
        set.addAll(classNames);
    }

    @Override
    public final Map<Class<? extends AppByteCodeScanner>, Set<String>> dependencyClasses() {
        return C.Map(dependencyClasses);
    }

}
