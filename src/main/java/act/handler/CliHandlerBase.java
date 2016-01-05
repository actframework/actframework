package act.handler;

import act.Act;
import act.app.CliContext;
import act.cli.builtin.Help;
import act.cli.util.CommandLineParser;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;

import java.util.List;
import java.util.Map;

public abstract class CliHandlerBase extends $.F1<CliContext, Void> implements CliHandler {

    private boolean destroyed;

    @Override
    public final Void apply(CliContext context) throws NotAppliedException, $.Break {
        handle(context);
        return null;
    }

    @Override
    public boolean appliedIn(Act.Mode mode) {
        return true;
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        releaseResources();
    }

    public String summary() {
        return "";
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}
}
