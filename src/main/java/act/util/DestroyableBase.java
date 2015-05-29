package act.util;

import act.Destroyable;

public abstract class DestroyableBase implements Destroyable {

    private boolean destroyed;

    @Override
    public final void destroy() {
        if (destroyed) {
            return;
        }
        releaseResources();
        destroyed = true;
    }

    @Override
    public final boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}

}
