package act.util;

import act.Act;
import act.Destroyable;
import org.osgl.logging.L;
import org.osgl.logging.Logger;

public abstract class DestroyableBase implements Destroyable {

    protected static final Logger logger = L.get(Act.class);

    private boolean destroyed;

    @Override
    public final void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        releaseResources();
    }

    @Override
    public final boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}

}
