package act.event;

import act.app.App;
import act.app.event.AppClassLoaded;
import act.app.event.AppCodeScanned;
import act.app.event.AppEventId;
import act.plugin.AppServicePlugin;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import org.osgl._;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class EventListenerClassFinder extends AppServicePlugin {
    @Override
    protected void applyTo(App app) {
        app.eventBus().bind(AppEventId.APP_CODE_SCANNED, new AppEventListenerBase<AppCodeScanned>() {
            @Override
            public void on(AppCodeScanned event) {
                final App app = event.source();
                ClassInfoRepository repo = (ClassInfoRepository)app.classLoader().classInfoRepository();
                ClassNode eventListener = repo.node(ActEventListener.class.getName());
                final EventBus bus = app.eventBus();
                eventListener.accept(new _.Visitor<ClassNode>() {
                    @Override
                    public void visit(ClassNode classNode) throws _.Break {
                        if (!classNode.publicNotAbstract()) return;
                        Class<?> c = _.classForName(classNode.name(), app.classLoader());
                        ParameterizedType ptype = null;
                        Type superType = c.getGenericSuperclass();
                        while (ptype == null) {
                            if (superType instanceof ParameterizedType) {
                                ptype = (ParameterizedType) superType;
                            } else {
                                if (Object.class == superType) {
                                    logger.warn("Event listener registration failed: cannot find generic information for %s", classNode.name());
                                    return;
                                }
                                superType = ((Class) superType).getGenericSuperclass();
                            }
                        }
                        Type[] ca = ptype.getActualTypeArguments();
                        for (Type t: ca) {
                            if (t instanceof Class) {
                                Class tc = (Class)t;
                                if (ActEvent.class.isAssignableFrom(tc)) {
                                    ActEventListener listener = (ActEventListener)app.newInstance(c);
                                    bus.bind(tc, listener);
                                    return;
                                }
                            }
                        }
                    }
                });
            }
        });
    }
}
