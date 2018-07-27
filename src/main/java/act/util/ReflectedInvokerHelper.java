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

import act.Act;
import act.app.App;
import act.app.AppServiceBase;
import org.osgl.$;
import org.osgl.inject.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.List;

public class ReflectedInvokerHelper {

    /**
     * If the `invokerClass` specified is singleton, or without field or all fields are
     * stateless, then return an instance of the invoker class. Otherwise, return null
     * @param invokerClass the invoker class
     * @param app the app
     * @return an instance of the invokerClass or `null` if invoker class is stateful class
     */
    public static Object tryGetSingleton(Class<?> invokerClass, App app) {
        Object singleton = app.singleton(invokerClass);
        if (null == singleton) {
            // check if there are fields
            List<Field> fields = $.fieldsOf(invokerClass);
            if (fields.isEmpty()) {
                singleton = app.getInstance(invokerClass);
            } else {
                boolean stateful = false;
                for (Field field : fields) {
                    if (!isGlobalOrStateless(field)) {
                        stateful = true;
                        break;
                    }
                }
                if (!stateful) {
                    singleton = app.getInstance(invokerClass);
                }
            }
        }
        if (null != singleton) {
            app.registerSingleton(singleton);
        }
        return singleton;
    }


    private static boolean isGlobalOrStateless(Field field) {
        if (null != field.getAnnotation(Stateless.class) || null != field.getAnnotation(Global.class) || null != field.getAnnotation(Configuration.class)) {
            return true;
        }
        Class<?> fieldType = field.getType();
        return Act.app().isSingleton(fieldType) || AppServiceBase.class.isAssignableFrom(fieldType);
    }

}
