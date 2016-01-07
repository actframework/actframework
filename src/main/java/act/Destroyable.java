package act;

import org.osgl.util.IO;

import java.io.Closeable;
import java.util.Collection;

public interface Destroyable {
    void destroy();
    boolean isDestroyed();

    public enum Util {
        ;

        /**
         * Helper method to destroy all elements in a in a {@link Destroyable} collection
         * @param col the collection contains {@link Destroyable} elements
         */
        public static void destroyAll(Collection<? extends Destroyable> col) {
            for (Destroyable e : col) {
                e.destroy();
            }
        }

        /**
         * Helper method to destroy all {@link Destroyable} elements,
         * and close all {@link Closeable} elements in the collection specified
         * @param col the collection might contains {@link Destroyable} and
         *            {@link Closeable} elements
         */
        public static void tryDestroyAll(Collection<?> col) {
            for (Object o: col) {
                if (o instanceof Destroyable) {
                    ((Destroyable) o).destroy();
                }
                if (o instanceof Closeable) {
                    IO.close((Closeable) o);
                }
            }
        }
    }
}
