package act.util;

import org.osgl.util.E;

import java.util.Iterator;

/**
 * Workaround for https://github.com/alibaba/fastjson/issues/478
 */
public class FastJsonIterable<T> implements Iterable<T> {

    private Iterable<T> it;

    public FastJsonIterable(Iterable<T> iterable) {
        E.illegalArgumentIf(iterable instanceof FastJsonIterable);
        it = iterable;
    }

    @Override
    public Iterator<T> iterator() {
        return it.iterator();
    }
}
