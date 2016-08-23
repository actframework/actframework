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
public abstract class SubTypeFinder<T> extends AppServicePlugin {

    protected static Logger logger = L.get(SubTypeFinder.class);

    private Class<T> targetType;
    private AppEventId bindingEvent = AppEventId.DEPENDENCY_INJECTOR_PROVISIONED;

    public SubTypeFinder(Class<T> target) {
        E.NPE(target);
        targetType = target;
    }

    public SubTypeFinder(Class<T> target, AppEventId bindingEvent) {
        this(target);
        this.bindingEvent = $.notNull(bindingEvent);
    }

    protected abstract void found(Class<? extends T> target, App app);

    @Override
    public final void applyTo(final App app) {
        app.eventBus().bind(bindingEvent, new AppEventListenerBase() {
            @Override
            public void on(EventObject event) throws Exception {
                ClassInfoRepository repo = app.classLoader().classInfoRepository();
                ClassNode parent = repo.node(targetType.getName());
                parent.visitPublicNotAbstractTreeNodes(new $.Visitor<ClassNode>() {
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
