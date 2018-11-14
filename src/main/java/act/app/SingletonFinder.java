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

import act.Act;
import act.app.event.SysEventId;
import act.util.*;
import org.osgl.$;
import org.osgl.inject.Injector;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import javax.inject.Singleton;

/**
 * Find all classes annotated with {@link javax.inject.Singleton}
 */
@SuppressWarnings("unused")
public class SingletonFinder {

    private static final Logger LOGGER = LogManager.get(SingletonFinder.class);

    private SingletonFinder() {}

    @SubClassFinder(SingletonBase.class)
    @AnnotatedClassFinder(Singleton.class)
    public static void found(Class<?> cls) {
        registerSingleton(cls);
    }

    @AnnotatedClassFinder(value = Stateless.class, callOn = SysEventId.PRE_START)
    public static void foundStateless(Class<?> cls) {
        registerSingleton(cls);
    }

    @AnnotatedClassFinder(value = InheritedStateless.class, callOn = SysEventId.PRE_START, noAbstract = false)
    public static void foundInheritedStateless(Class<?> cls) {
        final App app = App.instance();
        if (!Modifier.isAbstract(cls.getModifiers())) {
            app.registerSingletonClass(cls);
        }
        ClassInfoRepository repo = app.classLoader().classInfoRepository();
        ClassNode node = repo.node(cls.getName());
        node.visitPublicNotAbstractSubTreeNodes(new $.Visitor<ClassNode>() {
            @Override
            public void visit(ClassNode classNode) throws $.Break {
                String name = classNode.name();
                Class<?> cls = app.classForName(name);
                if (!stopInheritedScope(cls)) {
                    registerSingleton(cls);
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("@Stateful or @StopInheritedScope annotation found on %s, inherited stateless terminated", name);
                }
            }
        });
    }

    private static void registerSingleton(Class<?> cls) {
        E.invalidConfigurationIf(stopInheritedScope(cls), "@Stateful or @StopInheritedScope annotation cannot be apply on singleton or @Stateless annotated class");
        Object instance = tryLoadAppService(cls);
        if (null != instance) {
            Act.app().registerSingleton(instance);
        } else if (null == cls.getAnnotation(Lazy.class)) {
            App.instance().registerSingletonClass(cls);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("register singleton: %s", cls);
            }
        } else if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("skip lazy singleton registration for %s", cls);
        }
    }

    private static Object tryLoadAppService(Class cls) {
        return Act.app().service(cls);
    }

    private static boolean stopInheritedScope(Class<?> cls) {
        Injector injector = Act.app().injector();
        for (Annotation anno : cls.getAnnotations()) {
            if (injector.isInheritedScopeStopper(anno.annotationType())) {
                return true;
            }
        }
        return false;
    }

}

