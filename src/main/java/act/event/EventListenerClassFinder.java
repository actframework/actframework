package act.event;

import act.app.App;
import act.app.event.AppCodeScanned;
import act.app.event.AppEventId;
import act.plugin.AppServicePlugin;
import act.plugin.Extends;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import act.util.SubTypeFinder2;
import org.osgl._;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EventObject;

public class EventListenerClassFinder extends SubTypeFinder2<ActEventListener> {

    public EventListenerClassFinder() {
        super(ActEventListener.class);
    }

    @Override
    protected void found(final Class<ActEventListener> target, final App app) {
        final EventBus bus = app.eventBus();
        ParameterizedType ptype = null;
        Type superType = target.getGenericSuperclass();
        while (ptype == null) {
            if (superType instanceof ParameterizedType) {
                ptype = (ParameterizedType) superType;
            } else {
                if (Object.class == superType) {
                    logger.warn("Event listener registration failed: cannot find generic information for %s", target.getName());
                    return;
                }
                superType = ((Class) superType).getGenericSuperclass();
            }
        }
        Type[] ca = ptype.getActualTypeArguments();
        for (Type t : ca) {
            if (t instanceof Class) {
                final Class tc = (Class) t;
                if (ActEvent.class.isAssignableFrom(tc)) {
                    app.eventBus().bind(AppEventId.DEPENDENCY_INJECTOR_LOADED, new AppEventListenerBase() {
                        @Override
                        public void on(EventObject event) throws Exception {
                            ActEventListener listener = (ActEventListener) app.newInstance(target);
                            bus.bind(tc, listener);
                        }
                    });
                    return;
                }
            }
        }
    }
}
