package act.di.loader;

import act.app.App;
import act.di.BeanLoader;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;

/**
 * Load beans whose class extends/implements specified class/interface (as hint)
 */
public class SubClassBeanLoader extends BeanLoaderBase implements BeanLoader {

    /**
     * Loads a bean implementation which class implements/extends
     * the interface/class specified as `hint`.
     *
     * Note if there are multiple implementations matches `hint` then
     * the method will choose the first one it found to create the bean
     * instance
     *
     * @param hint the base class or interface class
     * @param options not used in this method
     * @return a bean instance whose class is a sub class or implementation of `hint`
     */
    @Override
    public Object load(Object hint, Object ... options) {
        E.illegalArgumentIf(!(hint instanceof Class));
        try {
            root((Class) hint).visitPublicNotAbstractSubTreeNodes(LOAD_ONE);
            return null;
        } catch (Osgl.Break e) {
            return e.get();
        }
    }

    /**
     * This method will load instances of all public and non-abstract classes that
     * implements/extends the interface/class specified as `hint`
     *
     * @param hint the base class or interface class
     * @param options not used in this method
     * @return the list of bean instances whose class is sub class or implementation of `hint`
     */
    @Override
    public List loadMultiple(Object hint, Object ... options) {
        final List list = C.newList();
        root((Class) hint).visitPublicNotAbstractSubTreeNodes(new $.Visitor<ClassNode>() {
            @Override
            public void visit(ClassNode classNode) throws Osgl.Break {
                list.add(app().newInstance(classNode.name()));
            }
        });
        return list;
    }

    /**
     * This method returns a predicate function that test the bean instance against the
     * class specified by `hint`. If the bean is an instance of the `hint` class, then
     * the predicate function returns `true` otherwise it returns `false`
     *
     * @param hint the base class or interface class
     * @param options Not used in this method
     * @return a predicate function whose behavior is described above
     */
    @Override
    public Osgl.Function filter(Object hint, Object... options) {
        final Class baseClass = (Class) hint;
        return new $.Predicate() {
            @Override
            public boolean test(Object o) {
                return baseClass.isAssignableFrom(o.getClass());
            }
        };
    }

    private ClassNode root(Class hint) {
        return app().classLoader().classInfoRepository().node(hint.getName());
    }

    private static final $.Visitor<ClassNode> LOAD_ONE = new $.Visitor<ClassNode>() {
        @Override
        public void visit(ClassNode classNode) throws Osgl.Break {
            throw new Osgl.Break(App.instance().newInstance(classNode.name()));
        }
    };


}
