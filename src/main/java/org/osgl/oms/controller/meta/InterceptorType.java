package org.osgl.oms.controller.meta;

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

    private static Map<Class<? extends Annotation>, InterceptorType> map = C.map(
            Before.class, BEFORE, After.class, AFTER,
            Catch.class, CATCH, Finally.class, FINALLY);

    public static InterceptorType of(Class<? extends Annotation> clz) {
        InterceptorType type = map.get(clz);
        E.illegalArgumentIf(null == type, "Not an interceptor annotation: %s", clz);
        return type;
    }
}
