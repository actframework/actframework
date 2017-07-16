package act.event;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.app.App;
import act.app.event.AppEvent;
import act.app.event.AppEventId;
import act.app.event.AppEventListener;
import act.util.SubTypeFinder;
import org.osgl.$;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EventObject;

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
                    AppEventId bindOn = AppEventId.START;
                    BindOn bindOnSpec = target.getAnnotation(BindOn.class);
                    if (null != bindOnSpec) {
                        bindOn = bindOnSpec.value();
                    }
                    app.eventBus().bind(bindOn, new AppEventListenerBase() {
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
