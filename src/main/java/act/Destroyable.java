package act;

import org.osgl.util.IO;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.Collection;

public interface Destroyable {
    void destroy();

    boolean isDestroyed();

    public Class<? extends Annotation> scope();

    public enum Util {
        ;

        /**
         * Helper method to destroy all elements in a in a {@link Destroyable} collection
         *
         * @param col the collection contains {@link Destroyable} elements
         * @deprecated use {@link #destroyAll(Collection, Class)} instead
         */
        public static void destroyAll(Collection<? extends Destroyable> col) {
            destroyAll(col, null);
        }

        public static void destroyAll(Collection<? extends Destroyable> col, Class<? extends Annotation> scope) {
            for (Destroyable e : col) {
                if (inScope(e, scope)) {
                    e.destroy();
                    if (e instanceof Closeable) {
                        IO.close((Closeable) e);
                    }
                }
            }
        }

        /**
         * Helper method to destroy all {@link Destroyable} elements,
         * and close all {@link Closeable} elements in the collection specified
         *
         * @param col the collection might contains {@link Destroyable} and
         *            {@link Closeable} elements
         */
        @Deprecated
        public static void tryDestroyAll(Collection<?> col) {
            tryDestroyAll(col, null);
        }

        /**
         * Helper method to destroy all {@link Destroyable} elements,
         * and close all {@link Closeable} elements in the collection specified
         *
         * @param col   the collection might contains {@link Destroyable} and
         *              {@link Closeable} elements
         * @param scope specify the scope annotation.
         */
        public static void tryDestroyAll(Collection<?> col, Class<? extends Annotation> scope) {
            if (null == col) {
                return;
            }
            for (Object o : col) {
                if (inScope(o, scope)) {
                    if (o instanceof Destroyable) {
                        ((Destroyable) o).destroy();
                    }
                    if (o instanceof Closeable) {
                        IO.close((Closeable) o);
                    }
                }
            }

            col.clear();
        }

        public static void tryDestroy(Object o) {
            tryDestroy(o, null);
        }

        public static void tryDestroy(Object o, Class<? extends Annotation> scope) {
            if (null == o) {
                return;
            }
            if (!inScope(o, scope)) {
                return;
            }
            if (o instanceof Destroyable) {
                ((Destroyable) o).destroy();
            }
            if (o instanceof Closeable) {
                IO.close((Closeable) o);
            }
        }

        private static boolean inScope(Object o, Class<? extends Annotation> scope) {
            if (null == o) {
                return false;
            }
            if (null == scope || scope == ApplicationScoped.class) {
                return true;
            }
            Class<?> c = o.getClass();
            if (c.isAnnotationPresent(scope)) {
                return true;
            }
            if (scope == SessionScoped.class) {
                // RequestScoped is always inside Session scope
                return c.isAnnotationPresent(RequestScoped.class);
            }
            return false;
        }
    }
}
