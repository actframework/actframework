package act.di.builder;

import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.TreeSet;
import java.util.Set;

public class TreeSetBuilder extends CollectionBuilder<TreeSet> {

    public TreeSetBuilder(Class<TreeSet> targetClass, Annotation[] annotations, Type[]typeParameters) {
        super(targetClass, annotations, typeParameters);
    }

    @Override
    public Set<Class<? extends TreeSet>> supportedClasses() {
        Set<Class<? extends TreeSet>> set = C.newSet();
        set.add(TreeSet.class);
        return set;
    }

    @Override
    protected TreeSet createInstance() {
        return new TreeSet();
    }
}
