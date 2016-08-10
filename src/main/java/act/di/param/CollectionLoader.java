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

class CollectionLoader implements ParamValueLoader {

    private final ParamKey key;
    private final Class<? extends Collection> collectionClass;
    private final Type elementType;
    private final DependencyInjector<?> injector;
    private final StringValueResolver resolver;
    private final Map<ParamKey, ParamValueLoader> childLoaders = new HashMap<>();
    private final ParamValueLoaderManager manager;

    CollectionLoader(
            ParamKey key,
            Class<? extends Collection> collection,
            Type elementType,
            DependencyInjector<?> injector,
            ParamValueLoaderManager manager
    ) {
        this.key = key;
        this.collectionClass = collection;
        this.elementType = elementType;
        this.injector = injector;
        this.manager = manager;
        this.resolver = App.instance().resolverManager().resolver(BeanSpec.rawTypeOf(elementType));
    }

    @Override
    public Object load(Object bean, ActContext context, boolean noDefaultValue) {
        ParamTree tree = ParamValueLoaderManager.ensureParamTree(context);
        ParamTreeNode node = tree.node(key);
        if (null == node) {
            return noDefaultValue ? null : injector.get(collectionClass);
        }
        Collection collection = null == bean ? injector.get(collectionClass) : (Collection) bean;
        if (node.isList()) {
            for (ParamTreeNode elementNode : node.list()) {
                if (!elementNode.isLeaf()) {
                    throw new BadRequest("cannot parse param: expect leaf node, found: \n%s", node.debug());
                }
                if (null == resolver) {
                    throw E.unexpected("Component type not resolvable: %s", elementType);
                }
                if (null != elementNode.value()) {
                    collection.add(resolver.resolve(elementNode.value()));
                }
            }
        } else if (node.isMap()) {
            Set<String> childrenKeys = node.mapKeys();
            for (String s : childrenKeys) {
                int id;
                try {
                    id = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    throw new BadRequest("cannot parse param: list index[%s] is not a number: %s", s);
                }
                ParamTreeNode child = node.child(s);
                if (child.isLeaf()) {
                    if (null == resolver) {
                        throw E.unexpected("Component type not resolvable: %s", elementType);
                    }
                    if (null != child.value()) {
                        addToCollection(collection, id, resolver.resolve(child.value()));
                    }
                } else {
                    ParamValueLoader childLoader = childLoader(child.key());
                    addToCollection(collection, id, childLoader.load(null, context, false));
                }
            }
        } else {
            if (null == resolver) {
                throw E.unexpected("Component type not resolvable: %s", elementType);
            }
            collection.add(resolver.resolve(node.value()));
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
        return manager.buildLoader(key, elementType, injector);
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
