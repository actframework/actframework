package act.handler;

import act.cli.CliContext;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;

public abstract class CliHandlerBase extends $.F1<CliContext, Void> implements CliHandler {

    private boolean destroyed;

    @Override
    public final Void apply(CliContext context) throws NotAppliedException, $.Break {
        handle(context);
        return null;
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        releaseResources();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}
}
