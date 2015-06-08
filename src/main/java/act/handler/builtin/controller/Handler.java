package act.handler.builtin.controller;

import org.osgl._;

/**
 * The base class of @Before, @After, @Exception, @Finally interceptor and
 * request dispatcher
 */
public abstract class Handler<T extends Handler> implements Comparable<T> {

    private int priority;

    protected Handler(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(T o) {
        return priority - o.priority();
    }

    @Override
    public int hashCode() {
        return _.hc(priority, getClass());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj.getClass().equals(getClass());
    }

    public void accept(Visitor visitor) {}

    public interface Visitor {
        ActionHandlerInvoker.Visitor invokerVisitor();
    }

}
