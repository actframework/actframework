package testapp.util;

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

import act.app.App;
import act.util.SingletonBase;

import javax.inject.Singleton;

@Singleton
public class MySingleton extends SingletonBase {
    public static MySingleton instance() {
        return App.instance().singleton(MySingleton.class);
    }

    public static <T> T instance2() {
        return (T)App.instance().singleton(MySingleton.class);
    }
}
