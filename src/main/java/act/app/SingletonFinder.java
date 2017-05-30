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

import act.app.event.AppEventId;
import act.util.*;
import org.osgl.$;
import org.osgl.Osgl;

import javax.inject.Singleton;

/**
 * Find all classes annotated with {@link javax.inject.Singleton}
 */
@SuppressWarnings("unused")
public class SingletonFinder {

    private SingletonFinder() {}

    @SubClassFinder(SingletonBase.class)
    @AnnotatedClassFinder(Singleton.class)
    public static void found(Class<?> cls) {
        registerSingleton(cls);
    }

    @AnnotatedClassFinder(value = Stateless.class, callOn = AppEventId.PRE_START)
    public static void foundStateless(Class<?> cls) {
        registerSingleton(cls);
    }

    @AnnotatedClassFinder(value = InheritedStateless.class, callOn = AppEventId.PRE_START)
    public static void foundInheritedStateless(Class<?> cls) {
        final App app = App.instance();
        app.registerSingletonClass(cls);
        ClassInfoRepository repo = app.classLoader().classInfoRepository();
        ClassNode node = repo.node(cls.getName());
        node.visitPublicNotAbstractSubTreeNodes(new Osgl.Visitor<ClassNode>() {
            @Override
            public void visit(ClassNode classNode) throws Osgl.Break {
                String name = classNode.name();
                Class<?> cls = $.classForName(name, app.classLoader());
                registerSingleton(cls);
            }
        });
    }

    private static void registerSingleton(Class<?> cls) {
        if (null == cls.getAnnotation(Lazy.class)) {
            App.instance().registerSingletonClass(cls);
        }
    }

}

