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

import act.Act;
import act.app.App;
import act.app.AppClassLoader;
import act.conf.AppConfig;
import act.inject.DependencyInjector;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.InjectException;
import org.osgl.inject.loader.AnnotatedElementLoader;
import org.osgl.inject.loader.ConfigurationValueLoader;
import org.osgl.inject.loader.TypedElementLoader;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Integrate Genie with ActFramework
 */
class GenieProviders {

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
            return new ConfigurationValueLoader() {

                @Override
                public Object get() {
                    final AppConfig appConfig = app().config();
                    DependencyInjector injector = Act.injector();
                    final String confKey = value().toString();
                    boolean isImpl = confKey.endsWith(".impl");
                    if (this.spec.isInstanceOf(Map.class)) {
                        String prefix = confKey.toString();
                        Map<String, Object> confMap = appConfig.subSet(prefix);
                        Map retVal = (Map) injector.get(spec.rawType());
                        List<Type> typeParams = spec.typeParams();
                        Type valType = null != typeParams && typeParams.size() > 1 ? typeParams.get(1) : Object.class;
                        BeanSpec valSpec = BeanSpec.of(valType, injector);
                        int pos = prefix.length() + 1;
                        for (String key : confMap.keySet()) {
                            if (S.eq(key, prefix)) {
                                continue;
                            }
                            Object val = confMap.get(key);
                            retVal.put(key.substring(pos), null == val ? null : cast(S.string(val), valSpec, isImpl));
                        }
                        return retVal;
                    } else if (this.spec.isInstanceOf(Collection.class) || this.spec.isArray()) {
                        Object val;
                        try {
                            val = appConfig.get(confKey);
                        } catch (Exception e) {
                            val = appConfig.rawConfiguration().get(confKey);
                        }
                        if (spec.isInstance(val)) {
                            return val;
                        }
                        return cast(null == val ? null : val.toString(), spec, isImpl);
                    } else {
                        if (S.isBlank(confKey)) {
                            throw new InjectException(("Missing configuration key"));
                        }
                        Object conf = conf(confKey);
                        if (null == conf) {
                            return null;
                        }
                        return cast(S.string(conf), spec, isImpl);
                    }
                }



                @Override
                protected Object conf(String s) {
                    return app().config().get(s);
                }

                private Object cast(String val, BeanSpec spec, boolean isImpl) {
                    if (null == val) {
                        return null;
                    }
                    if (spec.isInstanceOf(Collection.class)) {
                        Collection retVal = (Collection<?>) Act.getInstance(spec.rawType());
                        List<Type> typeParams = spec.typeParams();
                        Type itemType = (null != typeParams && typeParams.size() > 0) ? typeParams.get(0) : Object.class;
                        BeanSpec itemSpec = BeanSpec.of(itemType, Act.injector());
                        for (String itemVal : S.fastSplit(S.string(val), ",")) {
                            retVal.add(cast(itemVal, itemSpec, isImpl));
                        }
                        return retVal;
                    } else if (spec.isArray()) {
                        List list = new ArrayList();
                        Type itemType = spec.typeParams().get(0);
                        BeanSpec itemSpec = BeanSpec.of(itemType, Act.injector());
                        for (String itemVal : S.fastSplit(S.string(val), ",")) {
                            list.add(cast(itemVal, itemSpec, isImpl));
                        }
                        return ArrayLoader.listToArray(list, (Class<?>)itemType);
                    } else {
                        Class<?> type = spec.rawType();
                        if (type.isInstance(val)) {
                            return val;
                        }
                        if (isImpl) {
                            return $.newInstance(val, Act.app().classLoader());
                        }
                        if ($.isSimpleType(type)) {
                            return Act.app().resolverManager().resolve(val, type);
                        }
                        // try impl anyway
                        try {
                            return $.newInstance(val, Act.app().classLoader());
                        } catch (Exception e) {
                            throw new InjectException("Cannot cast value type[%s] to required type[%s]", val.getClass(), spec);
                        }
                    }
                }

            };
        }
    };

    public static final Provider<TypedElementLoader> TYPED_ELEMENT_LOADER = new Provider<TypedElementLoader>() {
        @Override
        public TypedElementLoader get() {
            return new TypedElementLoader() {
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
