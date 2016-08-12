package act.inject.param;

import act.app.App;
import act.app.data.StringValueResolverManager;
import act.inject.DependencyInjector;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.result.BadRequest;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

import java.lang.reflect.Type;
import java.util.*;

class MapLoader implements ParamValueLoader {

    private final ParamKey key;
    private final Class<? extends Map> mapClass;
    private final Class keyClass;
    private final Type valType;
    private final DependencyInjector<?> injector;
    private final StringValueResolver keyResolver;
    private final StringValueResolver valueResolver;
    private final Map<ParamKey, ParamValueLoader> childLoaders = new HashMap<>();
    private final ParamValueLoaderManager manager;

    MapLoader(
            ParamKey key,
            Class<? extends Map> mapClass,
            Type keyType,
            Type valType,
            DependencyInjector<?> injector,
            ParamValueLoaderManager manager
    ) {
        this.key = key;
        this.mapClass = mapClass;
        this.keyClass = BeanSpec.rawTypeOf(keyType);
        this.valType = valType;
        this.injector = injector;
        this.manager = manager;
        StringValueResolverManager resolverManager = App.instance().resolverManager();
        this.valueResolver = resolverManager.resolver(BeanSpec.rawTypeOf(valType));
        this.keyResolver = resolverManager.resolver(this.keyClass);
        if (null == keyResolver) {
            throw new IllegalArgumentException("Map key type not resolvable: " + keyClass.getName());
        }
    }

    @Override
    public Object load(Object bean, ActContext context, boolean noDefaultValue) {
        ParamTree tree = ParamValueLoaderManager.ensureParamTree(context);
        ParamTreeNode node = tree.node(key);
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
                if (null == valueResolver) {
                    throw E.unexpected("Component type not resolvable: %s", valType);
                }
                if (null != elementNode.value()) {
                    map.put(i, valueResolver.resolve(elementNode.value()));
                }
            }
        } else if (node.isMap()) {
            Set<String> childrenKeys = node.mapKeys();
            Class valClass = BeanSpec.rawTypeOf(valType);
            for (String s : childrenKeys) {
                ParamTreeNode child = node.child(s);
                Object key = s;
                if (String.class != keyClass) {
                    key = keyResolver.resolve(s);
                }
                if (child.isLeaf()) {
                    if (null == valueResolver) {
                        throw E.unexpected("Component type not resolvable: %s", valType);
                    }
                    String sval = child.value();
                    if (null == sval) {
                        continue;
                    }
                    if (valClass != String.class) {
                        Object value = valueResolver.resolve(sval);
                        if (!valClass.isInstance(value)) {
                            throw new BadRequest("Cannot load parameter, expected type: %s, found: %s", valClass, value.getClass());
                        }
                        map.put(key, value);
                    } else {
                        map.put(key, sval);
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
            throw new BadRequest("Cannot load parameter, expected map, found:%s", node.value());
        }
        return map;
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
        return manager.buildLoader(key, valType, injector);
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
}
