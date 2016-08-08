package act.di.param;

import act.app.App;
import act.di.DependencyInjector;
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
    private final Type keyType;
    private final Type valType;
    private final DependencyInjector<?> injector;
    private final StringValueResolver resolver;
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
        this.keyType = keyType;
        this.valType = valType;
        this.injector = injector;
        this.manager = manager;
        this.resolver = App.instance().resolverManager().resolver(BeanSpec.rawTypeOf(valType));
    }

    @Override
    public Object load(ActContext context) {
        Map map = injector.get(mapClass);
        ParamTree tree = ParamValueLoaderManager.ensureParamTree(context);
        ParamTreeNode node = tree.node(key);
        if (null == node) {
            return null;
        }
        if (node.isList()) {
            Class keyClass = BeanSpec.rawTypeOf(keyType);
            if (Integer.class != keyClass) {
                throw new BadRequest("cannot load list into map with key type: %s", keyType);
            }
            List<ParamTreeNode> list = node.list();
            for (int i = 0; i < list.size(); ++i) {
                ParamTreeNode elementNode = list.get(i);
                if (!elementNode.isLeaf()) {
                    throw new BadRequest("cannot parse param: expect leaf node, found: \n%s", node.debug());
                }
                if (null == resolver) {
                    throw E.unexpected("Component type not resolvable: %s", valType);
                }
                if (null != elementNode.value()) {
                    map.put(i, resolver.resolve(elementNode.value()));
                }
            }
        } else if (node.isMap()) {
            Set<String> childrenKeys = node.mapKeys();
            Class valClass = BeanSpec.rawTypeOf(valType);
            for (String s : childrenKeys) {
                ParamTreeNode child = node.child(s);
                if (child.isLeaf()) {
                    if (null == resolver) {
                        throw E.unexpected("Component type not resolvable: %s", valType);
                    }
                    Object value = child.value();
                    if (null == value) {
                        continue;
                    }
                    if (!valClass.isInstance(value)) {
                        throw new BadRequest("Cannot load parameter, expected type: %s, found: %s", valClass, value.getClass());
                    }
                    map.put(s, value);
                } else {
                    ParamValueLoader childLoader = childLoader(child.key());
                    Object value = childLoader.load(context);
                    if (null != value) {
                        if (!valClass.isInstance(value)) {
                            throw new BadRequest("Cannot load parameter, expected type: %s, found: %s", valClass, value.getClass());
                        }
                        map.put(s, value);
                    }
                }
            }
        } else {
            throw new BadRequest("Cannot load parameter, expected map, found:%s", node.value());
        }
        return map;
    }

    @Override
    public Object load(ActContext context, boolean noDefaultValue) {
        return load(context);
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
