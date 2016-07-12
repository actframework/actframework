package act.di.loader;

import act.app.App;
import act.di.BeanLoader;
import act.util.ClassNode;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Load beans whose class has been annotated by
 * a certain annotation class
 */
public class AnnotatedBeanLoader extends BeanLoaderBase implements BeanLoader {

    /**
     * This method will load a bean implementation which class has been
     * annotated with annotation class equals to `hint` specified.
     *
     * Note if there are multiple implementations annotated by `hint` then
     * the method will choose the first one it found to create the bean
     * instance
     *
     * @param hint the annotation class
     * @param options not used in this method
     * @return a bean instance which class has been annotated by `hint`
     */
    @Override
    public Object load(Object hint, Object ... options) {
        E.illegalArgumentIf(!(hint instanceof Class));
        try {
            root((Class) hint).visitPublicNotAbstractAnnotatedClasses(LOAD_ONE);
            return null;
        } catch (Osgl.Break e) {
            return e.get();
        }
    }

    /**
     * This method will load instances of all public and non-abstract classes that
     * has been annotated by annotation class specified as `hint`
     *
     * @param hint the hint to specify the bean instances to be loaded
     * @param options optional parameters specified to refine the loading process
     * @return the list of bean instances
     */
    @Override
    public List loadMultiple(Object hint, Object ... options) {
        final List list = C.newList();
        root((Class) hint).visitPublicNotAbstractAnnotatedClasses(new $.Visitor<ClassNode>() {
            @Override
            public void visit(ClassNode classNode) throws Osgl.Break {
                list.add(app().newInstance(classNode.name()));
            }
        });
        return list;
    }

    @Override
    public Osgl.Function filter(final Object hint, Object... options) {
        final Class<? extends Annotation> annoClass = $.cast(hint);
        return new Osgl.Predicate() {
            @Override
            public boolean test(Object o) {
                return o.getClass().getAnnotation(annoClass) != null;
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
