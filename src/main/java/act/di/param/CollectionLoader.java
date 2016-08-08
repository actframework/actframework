package act.di.param;

import act.di.DependencyInjector;
import act.util.ActContext;
import org.osgl.util.E;

import java.lang.reflect.Type;
import java.util.Collection;

class CollectionLoader implements ParamValueLoader {

    private final ParamKey key;
    private final Collection collection;
    private final Type elementType;
    private final DependencyInjector<?> injector;

    CollectionLoader(ParamKey key, Collection collection, Type elementType, DependencyInjector<?> injector) {
        this.key = key;
        this.collection = collection;
        this.elementType = elementType;
        this.injector = injector;
    }

    @Override
    public Object load(ActContext context) {
        ParamTree tree = ParamValueLoaderManager.ensureParamTree(context);
        ParamTreeNode node = tree.node(key);
        if (null == node) {
            return null;
        }
        if (node.isList()) {
            for (ParamTreeNode elementNode: node.list()) {

            }
        } else if (node.isMap()) {

        } else {

        }
        throw E.tbd();
    }
}
