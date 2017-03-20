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

import act.util.AnnotatedClassFinder;
import act.util.SingletonBase;
import act.util.SubClassFinder;

import javax.inject.Singleton;

/**
 * Find all classes annotated with {@link javax.inject.Singleton}
 */

public class SingletonFinder {

    private SingletonFinder() {}

    @SubClassFinder(SingletonBase.class)
    @AnnotatedClassFinder(Singleton.class)
    public static void found(Class<?> cls) {
        App.instance().registerSingletonClass(cls);
    }

}

