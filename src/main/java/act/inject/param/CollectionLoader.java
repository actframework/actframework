package act.inject.param;

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
import act.app.ActionContext;
import act.app.App;
import act.inject.DependencyInjector;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.util.Binder;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.lang.reflect.Type;
import java.util.*;

class CollectionLoader extends ParamValueLoader.JsonBodySupported {

    private final ParamKey key;
    private final Class<? extends Collection> collectionClass;
    private final Type elementType;
    private final DependencyInjector<?> injector;
    private final StringValueResolver resolver;
    private final Binder binder;
    private final Map<ParamKey, ParamValueLoader> childLoaders = new HashMap<ParamKey, ParamValueLoader>();
    private final ParamValueLoaderService manager;
    private final boolean isChar;
    private final BeanSpec targetSpec;

    CollectionLoader(
            ParamKey key,
            Class<? extends Collection> collection,
            Type elementType,
            BeanSpec targetSpec,
            DependencyInjector<?> injector,
            ParamValueLoaderService manager
    ) {
        this.key = key;
        this.collectionClass = collection;
        this.elementType = elementType;
        this.isChar = char.class == elementType || Character.class == elementType;
        this.injector = injector;
        this.manager = manager;
        App app = Act.app();
        Class<?> rawType = BeanSpec.rawTypeOf(elementType);
        this.binder = app.binderManager().binder(rawType);
        this.targetSpec = targetSpec;
        if (null == binder) {
            this.resolver = App.instance().resolverManager().resolver(rawType, targetSpec);
        } else {
            this.resolver = null;
        }
//        if (null == this.binder && null == this.resolver) {
//            throw new IllegalArgumentException(S.fmt("Cannot find binder and resolver for %s", elementType));
//        }
    }

    @Override
    public String toString() {
        return S.concat("collection loader[", bindName(), "]");
    }

    @Override
    public String bindName() {
        return key.toString();
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        ParamTree tree = ParamValueLoaderService.ensureParamTree(context);
        ParamTreeNode node = tree.node(key, context);
        if (null == node) {
            return injector.get(collectionClass);
        }
        Collection collection = null == bean ? injector.get(collectionClass) : (Collection) bean;
        if (node.isList()) {
            List<ParamTreeNode> nodes = node.list();
            if (nodes.size() > 0) {
                String value = nodes.get(0).value();
                for (int i = 0; i < nodes.size(); ++i) {
                    ParamTreeNode elementNode = nodes.get(i);
                    if (!elementNode.isLeaf()) {
                        throw new BadRequest("cannot parse param: expect leaf node, found: \n%s", node.debug());
                    }
                    context.attribute(ActionContext.ATTR_CURRENT_FILE_INDEX, i);
                    if (null != binder) {
                        collection.add(binder.resolve(null, elementNode.value(), context));
                    } else {
                        collection.add(resolver.resolve(elementNode.value()));
                    }
                }
                context.removeAttribute(ActionContext.ATTR_CURRENT_FILE_INDEX);
            }
        } else if (node.isMap()) {
            Set<String> childrenKeys = node.mapKeys();
            for (String s : childrenKeys) {
                int id;
                try {
                    id = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    throw new BadRequest("cannot parse param: list index is not a number: %s", s);
                }
                ParamTreeNode child = node.child(s);
                if (child.isLeaf()) {
                    if (null != child.value()) {
                        if (null != binder) {
                            addToCollection(collection, id, binder.resolve(null, child.value(), context));
                        } else {
                            addToCollection(collection, id, resolver.resolve(child.value()));
                        }
                    }
                } else {
                    ParamValueLoader childLoader = childLoader(child.key());
                    addToCollection(collection, id, childLoader.load(null, context, false));
                }
            }
        } else {
            resolveInto(collection, node.value(), context);
        }
        return collection;
    }

    private ParamValueLoader childLoader(ParamKey key) {
        ParamValueLoader loader = childLoaders.get(key);
        if (null == loader) {
            loader = buildChildLoader(key);
            childLoaders.put(key, loader);
        }
        return loader;
    }

    private ParamValueLoader buildChildLoader(ParamKey key) {
        return manager.buildLoader(key, targetSpec.componentSpec());
    }

    private static void addToCollection(Collection collection, int index, Object bean) {
        if (collection instanceof List) {
            addToList((List) collection, index, bean);
        } else {
            collection.add(bean);
        }
    }

    private static void addToList(List list, int index, Object bean) {
        while (list.size() < index + 1) {
            list.add(null);
        }
        list.set(index, bean);
    }

    private void resolveInto(Collection collection, String value, ActContext context) {
        if (S.blank(value)) {
            return;
        }
        // processing string like `[6000, 10000]`
        if (value.startsWith("[") && value.endsWith("]")) {
            value = S.strip(value, S.BRACKETS);
        }
        // support multiple path variables like /foo/id1,id2
        String[] sa = value.split("[,;]+");
        boolean isChar = this.isChar;
        for (String s : sa) {
            if (isChar) {
                char[] ca = s.toCharArray();
                for (char c: ca) {
                    collection.add(c);
                }
            } else {
                if (null != binder) {
                    collection.add(binder.resolve(null, s, context));
                } else {
                    collection.add(resolver.resolve(s));
                }
            }
        }
    }
}
