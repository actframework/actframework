package act;

import java.util.Collection;

public interface Destroyable {
    void destroy();
    boolean isDestroyed();

    public enum Util {
        ;
        public static void destroyAll(Collection<? extends Destroyable> col) {
            for (Destroyable e : col) {
                e.destroy();
            }
        }
    }
}
