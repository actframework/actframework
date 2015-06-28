package act.util;

import java.util.Comparator;

/**
 * Used to sort Exception based on the inheritance hierarchy
 */
public class ExceptionComparator implements Comparator<Class<? extends Exception>> {
    @Override
    public int compare(Class<? extends Exception> o1, Class<? extends Exception> o2) {
        return hierarchicalLevel(o2) - hierarchicalLevel(o1);
    }

    private static int hierarchicalLevel(Class<? extends Exception> e) {
        int i = 0;
        Class<?> c = e;
        while (null != c) {
            i++;
            c = c.getSuperclass();
        }
        return i;
    }
}
