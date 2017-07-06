package act.inject.genie;

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
import act.app.AppClassLoader;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.inject.loader.AnnotatedElementLoader;
import org.osgl.inject.loader.ConfigurationValueLoader;
import org.osgl.inject.loader.TypedElementLoader;
import org.osgl.util.C;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Integrate Genie with ActFramework
 */
class GenieProviders {

    private static ConfigurationValueLoader _CONF_VAL_LOADER = new ConfigurationValueLoader() {
        @Override
        protected Object conf(String s) {
            return app().config().get(s);
        }
    };

    private static final TypedElementLoader _TYPED_ELEMENT_LOADER = new TypedElementLoader() {
        @Override
        protected List<Class> load(Class aClass, final boolean loadNonPublic, final boolean loadAbstract, final boolean loadRoot) {
            final AppClassLoader cl = app().classLoader();
            final ClassNode root = cl.classInfoRepository().node(aClass.getName());
            if (null == root) {
                return C.list();
            }
            final List<Class> list = C.newList();
            Osgl.Visitor<ClassNode> visitor = new Osgl.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws Osgl.Break {
                    Class c = $.classForName(classNode.name(), cl);
                    list.add(c);
                }
            };
            root.visitTree($.guardedVisitor(new $.Predicate<ClassNode>() {
                @Override
                public boolean test(ClassNode classNode) {
                    if (!loadNonPublic && !classNode.isPublic()) {
                        return false;
                    }
                    if (!loadAbstract && classNode.isAbstract()) {
                        return false;
                    }
                    if (!loadRoot && root == classNode) {
                        return false;
                    }
                    return true;
                }
            }, visitor));
            return list;
        }

    };

    private static final AnnotatedElementLoader _ANNO_ELEMENT_LOADER = new AnnotatedElementLoader() {
        @Override
        protected List<Class<?>> load(Class<? extends Annotation> aClass, final boolean loadNonPublic, final boolean loadAbstract) {
            final AppClassLoader cl = app().classLoader();
            ClassNode root = cl.classInfoRepository().node(aClass.getName());
            if (null == root) {
                return C.list();
            }
            final List<Class<?>> list = C.newList();
            Osgl.Visitor<ClassNode> visitor = new Osgl.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws Osgl.Break {
                    Class c = $.classForName(classNode.name(), cl);
                    list.add(c);
                }
            };
            for (ClassNode node : root.annotatedClasses()) {
                $.guardedVisitor(new $.Predicate<ClassNode>() {
                    @Override
                    public boolean test(ClassNode classNode) {
                        if (!loadNonPublic && !classNode.isPublic()) {
                            return false;
                        }
                        if (!loadAbstract && classNode.isAbstract()) {
                            return false;
                        }
                        return true;
                    }
                }, visitor).visit(node);
            }
            return list;
        }
    };

    private GenieProviders() {
    }

    public static final Provider<ConfigurationValueLoader> CONF_VALUE_LOADER = new Provider<ConfigurationValueLoader>() {
        @Override
        public ConfigurationValueLoader get() {
            return _CONF_VAL_LOADER;
        }
    };

    public static final Provider<TypedElementLoader> TYPED_ELEMENT_LOADER = new Provider<TypedElementLoader>() {
        @Override
        public TypedElementLoader get() {
            return _TYPED_ELEMENT_LOADER;
        }
    };

    public static final Provider<AnnotatedElementLoader> ANNOTATED_ELEMENT_LOADER = new Provider<AnnotatedElementLoader>() {
        @Override
        public AnnotatedElementLoader get() {
            return _ANNO_ELEMENT_LOADER;
        }
    };

    private static App app() {
        return App.instance();
    }

}
