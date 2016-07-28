package act.util;

import act.Destroyable;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import java.lang.annotation.Annotation;
import java.util.List;

public abstract class DestroyableBase implements Destroyable {

    private boolean destroyed;

    private List<Destroyable> subResources = C.newList();

    private volatile Class<? extends Annotation> scope;

    @Override
    public synchronized final void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        Destroyable.Util.destroyAll(subResources, scope());
        releaseResources();
    }

    @Override
    public final boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}

    public Class<? extends Annotation> scope() {
        if (null == scope) {
            synchronized (this) {
                if (null == scope) {
                    Class<?> c = getClass();
                    if (c.isAnnotationPresent(RequestScoped.class)) {
                        scope = RequestScoped.class;
                    } else if (c.isAnnotationPresent(SessionScoped.class)) {
                        scope = SessionScoped.class;
                    } else if (c.isAnnotationPresent(ApplicationScoped.class)) {
                        scope = ApplicationScoped.class;
                    } else {
                        scope = NormalScope.class;
                    }
                }
            }
        }
        return scope;
    }

    public synchronized void addSubResource(Destroyable object) {
        subResources.add(object);
    }

}
