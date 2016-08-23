package act.handler;

import act.Act;
import act.app.CliContext;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;
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

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
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
