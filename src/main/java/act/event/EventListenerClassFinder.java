package act.event;

import act.ActComponent;
import act.app.App;
import act.app.event.AppEvent;
import act.app.event.AppEventId;
import act.app.event.AppEventListener;
import act.util.SubTypeFinder;
import org.osgl.$;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EventObject;

@ActComponent
public class EventListenerClassFinder extends SubTypeFinder<ActEventListener> {

    public EventListenerClassFinder() {
        super(ActEventListener.class);
    }

    @Override
    protected void found(final Class<? extends ActEventListener> target, final App app) {
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
                if (AppEvent.class.isAssignableFrom(tc)) {
                    AppEvent prototype = $.cast($.newInstance(tc, app));
                    AppEventListener listener = $.cast(app.getInstance(target));
                    app.eventBus().bind(AppEventId.values()[prototype.id()], listener);
                } else if (ActEvent.class.isAssignableFrom(tc)) {
                    app.eventBus().bind(AppEventId.START, new AppEventListenerBase() {
                        @Override
                        public void on(EventObject event) throws Exception {
                            ActEventListener listener = app.getInstance(target);
                            bus.bind(tc, listener);
                        }
                    });
                    return;
                }
            }
        }
    }
}
