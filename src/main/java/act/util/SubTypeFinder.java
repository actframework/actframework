package act.util;

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
import act.app.event.SysEventId;
import act.event.SysEventListenerBase;
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
public abstract class SubTypeFinder<T> extends AppServicePlugin {

    protected static Logger logger = L.get(SubTypeFinder.class);

    private Class<T> targetType;
    private SysEventId bindingEvent = SysEventId.DEPENDENCY_INJECTOR_PROVISIONED;

    public SubTypeFinder(Class<T> target) {
        E.NPE(target);
        targetType = target;
    }

    public SubTypeFinder(Class<T> target, SysEventId bindingEvent) {
        this(target);
        this.bindingEvent = $.requireNotNull(bindingEvent);
    }

    protected abstract void found(Class<? extends T> target, App app);

    @Override
    public final void applyTo(final App app) {
        app.eventBus().bind(bindingEvent, new SysEventListenerBase() {
            @Override
            public void on(EventObject event) throws Exception {
                ClassInfoRepository repo = app.classLoader().classInfoRepository();
                ClassNode parent = repo.node(targetType.getName());
                parent.visitPublicNotAbstractTreeNodes(new $.Visitor<ClassNode>() {
                    @Override
                    public void visit(ClassNode classNode) throws $.Break {
                        final Class<T> c = app.classForName(classNode.name());
                        if (!c.isAnnotationPresent(NoAutoRegister.class)) {
                            found(c, app);
                        }
                    }
                });
            }
        });
    }
}
