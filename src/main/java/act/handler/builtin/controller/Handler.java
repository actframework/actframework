package act.handler.builtin.controller;

import act.util.DestroyableBase;
import org.osgl.$;

/**
 * The base class of @Before, @After, @Exception, @Finally interceptor and
 * request dispatcher
 */
public abstract class Handler<T extends Handler> extends DestroyableBase implements Comparable<T> {

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
        return $.hc(priority, getClass());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj.getClass().equals(getClass());
    }

    public void accept(Visitor visitor) {}

    public abstract boolean sessionFree();

    public interface Visitor {
        ActionHandlerInvoker.Visitor invokerVisitor();
    }

    @Override
    protected void releaseResources() {
    }
}
