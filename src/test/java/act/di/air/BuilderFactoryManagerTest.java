package act.di.air;

import act.TestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Test {@link act.di.air.Builder.Factory.Manager}
 */
public class BuilderFactoryManagerTest extends TestBase {
    private static class ColBuilder extends Builder<AbstractCollection> {

        public ColBuilder(Class<? extends AbstractCollection> targetClass, Annotation[] annotations, Type[] typeParameters) {
            super(targetClass, annotations, typeParameters);
        }

        @Override
        protected AbstractCollection createInstance() {
            return new HashSet();
        }

        @Override
        protected void initializeInstance(AbstractCollection instance) {
            instance.add("ColBuilder");
        }

        static class Factory implements Builder.Factory<AbstractCollection> {
            @Override
            public Builder<AbstractCollection> createBuilder(Class<AbstractCollection> targetClass, Annotation[] annotations, Type[] typeParams) {
                return new ColBuilder(targetClass, annotations, typeParams);
            }

            @Override
            public Class<AbstractCollection> targetClass() {
                return AbstractCollection.class;
            }
        }
    }

    private static class ListBuilder extends Builder<AbstractList> {

        public ListBuilder(Class<? extends AbstractList> targetClass, Annotation[] annotations, Type[] typeParameters) {
            super(targetClass, annotations, typeParameters);
        }

        @Override
        protected AbstractList createInstance() {
            return new ArrayList();
        }

        @Override
        protected void initializeInstance(AbstractList instance) {
            instance.add("ListBuilder");
        }

        static class Factory implements Builder.Factory<AbstractList> {
            @Override
            public Builder<AbstractList> createBuilder(Class<AbstractList> targetClass, Annotation[] annotations, Type[] typeParams) {
                return new ListBuilder(targetClass, annotations, typeParams);
            }

            @Override
            public Class<AbstractList> targetClass() {
                return AbstractList.class;
            }
        }
    }

    @Before
    public void setup() {
        Builder.Factory.Manager.found(ColBuilder.Factory.class);
        Builder.Factory.Manager.found(ListBuilder.Factory.class);
    }

    @After
    public void teardown() {
        Builder.Factory.Manager.destroy();
    }

    @Test
    public void ColListBuilderShallTakePrecedenceOfListBuilderForCollection() {
        Builder.Factory<Collection> factory = Builder.Factory.Manager.get(Collection.class);
        assertNotNull(factory);
        Builder<Collection> builder = factory.createBuilder(Collection.class, null, null);
        Collection c = builder.get();
        yes(c instanceof Set);
    }

    @Test
    public void ListBuilderShallBeUsedForListInjection() {
        Builder.Factory<List> factory = Builder.Factory.Manager.get(List.class);
        assertNotNull(factory);
        Builder<List> builder = factory.createBuilder(List.class, null, null);
        List l = builder.get();
        assertNotNull(l);
        eq("ListBuilder", l.get(0));
    }
}
