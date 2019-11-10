package act.inject;

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

import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.app.event.SysEventId;
import act.util.ClassNode;
import act.util.SubClassFinder;
import org.osgl.$;
import org.osgl.inject.BeanSpec;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

public abstract class DependencyInjectorBase<DI extends DependencyInjectorBase<DI>> extends AppServiceBase<DI> implements DependencyInjector<DI> {

    protected Map<Class, DependencyInjectionBinder> binders = new HashMap<>();
    protected Map<Class, List<DependencyInjectionListener>> listeners = new HashMap<>();

    public DependencyInjectorBase(App app) {
        this(app, true);
    }

    protected DependencyInjectorBase(App app, boolean noRegister) {
        super(app, true);
        if (!noRegister) {
            app.injector(this);
        }
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.tryDestroyAll(binders.values(), ApplicationScoped.class);
        binders.clear();
    }


    @Override
    public synchronized void registerDiBinder(DependencyInjectionBinder binder) {
        binders.put(binder.targetClass(), binder);
        ActProviders.addProvidedType(binder.targetClass());
    }

    @Override
    public synchronized void registerDiListener(DependencyInjectionListener listener) {
        Class[] targets = listener.listenTo();
        for (Class c : targets) {
            List<DependencyInjectionListener> list = listeners.get(c);
            if (null == list) {
                final List<DependencyInjectionListener> list0 = new ArrayList<>();
                list = list0;
                final App app = app();
                ClassNode node = app.classLoader().classInfoRepository().node(c.getName());
                node.visitPublicNotAbstractTreeNodes(new $.Visitor<ClassNode>() {
                    @Override
                    public void visit(ClassNode classNode) throws $.Break {
                        listeners.put(app.classForName(classNode.name()), list0);
                    }
                });
                listeners.put(c, list);
            }
            list.add(listener);
        }
    }

    @Override
    public void fireInjectedEvent(Object bean, BeanSpec spec) {
        Class c = spec.rawType();
        List<DependencyInjectionListener> list = listeners.get(c);
        if (null != list) {
            for (DependencyInjectionListener listener : list) {
                listener.onInjection(bean, spec);
            }
        }
    }


    @SubClassFinder(value = DependencyInjectionListener.class, callOn = SysEventId.DEPENDENCY_INJECTOR_PROVISIONED)
    public static void discoverDiListener(final Class<? extends DependencyInjectionListener> target) {
        App app = App.instance();
        DependencyInjector di = app.injector();
        di.registerDiListener(app.getInstance(target));
    }

}
