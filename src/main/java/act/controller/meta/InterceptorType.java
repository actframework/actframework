package act.controller.meta;

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

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.Catch;
import org.osgl.mvc.annotation.Finally;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.util.Map;

public enum InterceptorType {
    BEFORE() {
        @Override
        void addToGroup(InterceptorMethodMetaInfo info, GroupInterceptorMetaInfo group) {
            group.addBefore(info);
        }
    }, AFTER() {
        @Override
        void addToGroup(InterceptorMethodMetaInfo info, GroupInterceptorMetaInfo group) {
            group.addAfter(info);
        }
    }, CATCH() {
        @Override
        void addToGroup(InterceptorMethodMetaInfo info, GroupInterceptorMetaInfo group) {
            group.addCatch((CatchMethodMetaInfo) info);
        }

        @Override
        public InterceptorMethodMetaInfo createMetaInfo(ControllerClassMetaInfo clsInfo) {
            return new CatchMethodMetaInfo(clsInfo);
        }
    }, FINALLY() {
        @Override
        void addToGroup(InterceptorMethodMetaInfo info, GroupInterceptorMetaInfo group) {
            group.addFinally(info);
        }
    };

    abstract void addToGroup(InterceptorMethodMetaInfo info, GroupInterceptorMetaInfo group);

    public InterceptorMethodMetaInfo createMetaInfo(ControllerClassMetaInfo clsInfo) {
        return new InterceptorMethodMetaInfo(clsInfo);
    }

    private static Map<Class<? extends Annotation>, InterceptorType> map = C.Map(
            Before.class, BEFORE, After.class, AFTER,
            Catch.class, CATCH, Finally.class, FINALLY);

    public static InterceptorType of(Class<? extends Annotation> clz) {
        InterceptorType type = map.get(clz);
        E.illegalArgumentIf(null == type, "Not an interceptor annotation: %s", clz);
        return type;
    }
}
