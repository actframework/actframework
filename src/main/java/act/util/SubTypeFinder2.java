package act.util;

import act.ActComponent;
import act.app.App;
import act.app.event.AppEventId;
import act.event.AppEventListenerBase;
import act.plugin.AppServicePlugin;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;

import java.util.EventObject;

/**
 * Find classes that extends a specified type directly
 * or indirectly, or implement a specified type directly
 * or indirectly
 */
@ActComponent
public abstract class SubTypeFinder2<T> extends AppServicePlugin {

    protected static Logger logger = L.get(SubTypeFinder2.class);

    private Class<T> targetType;
    private App app;

    public SubTypeFinder2(Class<T> target) {
        E.NPE(target);
        targetType = target;
    }

    protected abstract void found(Class<T> target, App app);

    @Override
    final protected void applyTo(final App app) {
        app.eventBus().bind(AppEventId.APP_CODE_SCANNED, new AppEventListenerBase() {
            @Override
            public void on(EventObject event) throws Exception {
                ClassInfoRepository repo = app.classLoader().classInfoRepository();
                ClassNode parent = repo.node(targetType.getName());
                parent.findPublicNotAbstract(new $.Visitor<ClassNode>() {
                    @Override
                    public void visit(ClassNode classNode) throws $.Break {
                        final Class<T> c = $.classForName(classNode.name(), app.classLoader());
                        found(c, app);
                    }
                });
            }
        });
    }
}
