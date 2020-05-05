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

import act.app.ActionContext;
import act.app.App;
import act.app.data.StringValueResolverManager;
import act.inject.DependencyInjector;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.result.BadRequest;
import org.osgl.util.*;

import java.lang.reflect.Type;
import java.util.*;

class MapLoader extends ParamValueLoader.JsonBodySupported {

    private final ParamKey key;
    private final Class<? extends Map> mapClass;
    private final Class keyClass;
    private final Type valType;
    private final DependencyInjector<?> injector;
    private final StringValueResolver keyResolver;
    private final StringValueResolver valueResolver;
    private final Map<ParamKey, ParamValueLoader> childLoaders = new HashMap<ParamKey, ParamValueLoader>();
    private final ParamValueLoaderService manager;
    private final BeanSpec targetSpec;
    private final boolean valTypeIsObject;

    MapLoader(
            ParamKey key,
            Class<? extends Map> mapClass,
            Type keyType,
            Type valType,
            BeanSpec targetSpec,
            DependencyInjector<?> injector,
            ParamValueLoaderService manager
    ) {
        this.key = key;
        this.mapClass = mapClass;
        this.keyClass = BeanSpec.rawTypeOf(keyType);
        this.valType = valType;
        this.valTypeIsObject = Object.class == valType;
        this.injector = injector;
        this.manager = manager;
        this.targetSpec = targetSpec;
        StringValueResolverManager resolverManager = App.instance().resolverManager();
        BeanSpec valSpec = BeanSpec.of(valType, injector);
        Class<?> valClass = valSpec.rawType();
        if (!this.valTypeIsObject) {
            if (Collection.class.isAssignableFrom(valClass)) {
                Class<? extends Collection> colClass = $.cast(valClass);
                this.valueResolver = resolverManager.collectionResolver(colClass, (Class<?>) valSpec.typeParams().get(0), ',');
            } else if (valClass.isArray()) {
                this.valueResolver = resolverManager.arrayResolver(valClass, ',');
            } else {
                this.valueResolver = resolverManager.resolver(valClass, BeanSpec.of(valType, injector));
            }
            if (null == valueResolver) {
                warn("Map value type not resolvable: " + valClass.getName());
            }
        } else {
            valueResolver = null;
        }
        this.keyResolver = resolverManager.resolver(this.keyClass, BeanSpec.of(this.keyClass, injector));
        if (null == keyResolver) {
            throw new IllegalArgumentException("Map key type not resolvable: " + keyClass.getName());
        }
    }

    @Override
    public String toString() {
        return S.concat("map loader[", bindName(), "]");
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        ParamTree tree = ParamValueLoaderService.ensureParamTree(context);
        ParamTreeNode node = tree.node(key, context);
        if (null == node) {
            return noDefaultValue ? null : injector.get(mapClass);
        }
        Map map = null == bean ? injector.get(mapClass) : (Map) bean;
        if (node.isList()) {
            if (Integer.class != keyClass) {
                throw new BadRequest("cannot load list into map with key type: %s", this.keyClass);
            }
            List<ParamTreeNode> list = node.list();
            for (int i = 0; i < list.size(); ++i) {
                ParamTreeNode elementNode = list.get(i);
                if (!elementNode.isLeaf()) {
                    throw new BadRequest("cannot parse param: expect leaf node, found: \n%s", node.debug());
                }
                if (valTypeIsObject) {
                    map.put(i, elementNode.value());
                } else  if (null == valueResolver) {
                    throw E.unexpected("Component type not resolvable: %s", valType);
                } else {
                    if (null != elementNode.value()) {
                        map.put(i, valueResolver.resolve(elementNode.value()));
                    }
                }
            }
        } else if (node.isMap()) {
            Set<String> childrenKeys = node.mapKeys();
            Class valClass = BeanSpec.rawTypeOf(valType);
            ActionContext actionContext = context instanceof ActionContext ? (ActionContext) context : null;
            for (String s : childrenKeys) {
                ParamTreeNode child = node.child(s);
                Object key = s;
                if (String.class != keyClass) {
                    key = keyResolver.resolve(s);
                }
                if (child.isLeaf()) {
                    String sval = child.value();
                    if (null == sval) {
                        continue;
                    }
                    if (valTypeIsObject || String.class == valClass) {
                        if (null != actionContext && actionContext.isPathVar(s)) {
                            continue;
                        }
                        map.put(key, sval);
                    } else {
                        if (null == valueResolver) {
                            throw E.unexpected("Component type not resolvable: %s", valType);
                        }
                        Object value = valueResolver.resolve(sval);
                        if (null == value) {
                            continue;
                        }
                        if (!valClass.isInstance(value)) {
                            throw new BadRequest("Cannot load parameter, expected type: %s, found: %s", valClass, value.getClass());
                        }
                        map.put(key, value);
                    }
                } else {
                    ParamValueLoader childLoader = childLoader(child.key());
                    Object value = childLoader.load(null, context, false);
                    if (null != value) {
                        if (!valClass.isInstance(value)) {
                            throw new BadRequest("Cannot load parameter, expected type: %s, found: %s", valClass, value.getClass());
                        }
                        map.put(key, value);
                    }
                }
            }
        } else {
            // try evaluate the Map instance from string value
            // to support Matrix style URL path variable
            String value = node.value();
            if (S.notBlank(value)) {
                // ";" has higher priority in common separators
                String[] pairs = value.split(value.contains(";") ?  ";" : S.COMMON_SEP);
                for (String pair : pairs) {
                    if (!pair.contains("=")) {
                        throw new BadRequest("Cannot load parameter, expected map, found:%s", node.value());
                    }
                    String sKey = S.beforeFirst(pair, "=");
                    String sVal = S.afterFirst(pair, "=");
                    Object oKey = keyResolver.resolve(sKey);
                    Object oVal = valueResolver.resolve(sVal);
                    map.put(oKey, oVal);
                }
            }
        }
        return map;
    }

    @Override
    public String bindName() {
        return key.toString();
    }

    @Override
    public boolean supportJsonDecorator() {
        return true;
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
        return manager.buildLoader(key, valueSpec());
    }

    private BeanSpec valueSpec() {
        List<Type> typeParams = targetSpec.typeParams();
        E.illegalArgumentIf(typeParams.size() != 2);
        return BeanSpec.of(typeParams.get(1), targetSpec.injector());
    }
}
