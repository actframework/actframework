package act.controller.meta;

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

import static act.Destroyable.Util.destroyAll;

import act.Act;
import act.app.App;
import act.app.AppClassLoader;
import act.app.event.SysEventId;
import act.asm.Type;
import act.util.*;

import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ControllerClassMetaInfoManager extends LogSupportedDestroyableBase {

    private Map<String, ControllerClassMetaInfo> controllers = new HashMap<>();

    @Inject
    public ControllerClassMetaInfoManager(App app) {
        app.jobManager().on(SysEventId.APP_CODE_SCANNED, "ControllerClassMetaInfoManager:buildControllerHierarchies", new Runnable() {
            @Override
            public void run() {
                buildControllerHierarchies();
            }
        });
    }

    @Override
    protected void releaseResources() {
        destroyAll(controllers.values(), ApplicationScoped.class);
        controllers.clear();
        super.releaseResources();
    }

    public void registerControllerMetaInfo(ControllerClassMetaInfo metaInfo) {
        String className = Type.getObjectType(metaInfo.className()).getClassName();
        controllers.put(className, metaInfo);
        trace("Controller meta info registered for: %s", className);
    }

    public ControllerClassMetaInfo controllerMetaInfo(String className) {
        return controllers.get(className);
    }

    public void mergeActionMetaInfo(App app) {
        for (ControllerClassMetaInfo info : controllers.values()) {
            info.merge(this, app);
        }
    }

    public void buildControllerHierarchies() {
        AppClassLoader cl = Act.app().classLoader();
        ControllerClassMetaInfoManager manager = cl.controllerClassMetaInfoManager();
        ClassInfoRepository repo = cl.classInfoRepository();
        for (ControllerClassMetaInfo info : manager.controllers.values()) {
            buildSuperClassMetaInfo(info, manager, repo);
        }
    }

    private static void buildSuperClassMetaInfo(ControllerClassMetaInfo info, ControllerClassMetaInfoManager manager, ClassInfoRepository repo) {
        String className = info.className();
        ClassNode node = repo.node(className);
        if (null == node) {
            return;
        }
        ClassNode parent = node.parent();
        final String OBJECT = Object.class.getName();
        while (null != parent && !OBJECT.equals(parent.name())) {
            ControllerClassMetaInfo parentInfo = manager.controllerMetaInfo(parent.name());
            if (null != parentInfo) {
                info.parent(parentInfo);
                break;
            }
            parent = parent.parent();
        }
    }

}
