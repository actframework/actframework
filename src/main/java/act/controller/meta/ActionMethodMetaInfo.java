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

import act.app.App;
import act.asm.Type;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class ActionMethodMetaInfo extends HandlerMethodMetaInfo<ActionMethodMetaInfo> {
    private GroupInterceptorMetaInfo interceptors = new GroupInterceptorMetaInfo();
    private C.Set<String> withList = C.newSet();

    public ActionMethodMetaInfo(ControllerClassMetaInfo classMetaInfo) {
        super(classMetaInfo);
    }

    public ActionMethodMetaInfo(ActionMethodMetaInfo parentAction, ControllerClassMetaInfo thisClass) {
        super(parentAction, thisClass);
        this.interceptors.mergeFrom(thisClass.interceptors());
    }

    public ActionMethodMetaInfo addWith(String... classes) {
        int len = classes.length;
        if (len > 0) {
            for (int i = 0; i < len; ++i) {
                _addWith(classes[i]);
            }
        }
        return this;
    }

    @Override
    protected void releaseResources() {
        withList.clear();
        interceptors.destroy();
        super.releaseResources();
    }

    public HandlerMethodMetaInfo merge(ControllerClassMetaInfoManager infoBase, App app) {
        mergeFromWithList(infoBase, app);
        return this;
    }


    public ActionMethodMetaInfo mergeFromClassInterceptors(GroupInterceptorMetaInfo info) {
        interceptors.mergeFrom(info, name());
        return this;
    }

    public GroupInterceptorMetaInfo interceptors() {
        return interceptors;
    }

    public List<InterceptorMethodMetaInfo> beforeInterceptors() {
        return interceptors.beforeList();
    }

    public List<InterceptorMethodMetaInfo> afterInterceptors() {
        return interceptors.afterList();
    }

    public List<CatchMethodMetaInfo> exceptionInterceptors() {
        return interceptors.catchList();
    }

    public List<InterceptorMethodMetaInfo> finallyInterceptors() {
        return interceptors.finallyList();
    }

    @Override
    public String toString() {
        return toStrBuffer(S.newBuffer()).toString();
    }

    @Override
    protected S.Buffer toStrBuffer(S.Buffer buffer) {
        return super.toStrBuffer(buffer).append("\n").append(interceptors);
    }

    private void _addWith(String clsName) {
        withList.add(Type.getType(clsName).getClassName());
    }

    private void mergeFromWithList(final ControllerClassMetaInfoManager infoBase, final App app) {
        C.Set<String> withClasses = this.withList;
        if (withClasses.isEmpty()) {
            return;
        }
        ClassInfoRepository repo = app.classLoader().classInfoRepository();
        for (final String withClass : withClasses) {
            String curWithClass = withClass;
            ControllerClassMetaInfo withClassInfo = infoBase.controllerMetaInfo(curWithClass);
            while (null == withClassInfo && !"java.lang.Object".equals(curWithClass)) {
                ClassNode node = repo.node(curWithClass);
                if (null != node) {
                    node = node.parent();
                }
                if (null == node) {
                    break;
                }
                curWithClass = node.name();
                withClassInfo = infoBase.controllerMetaInfo(curWithClass);
            }
            if (null != withClassInfo) {
                withClassInfo.merge(infoBase, app);
                interceptors.mergeFrom(withClassInfo.interceptors);
            }
        }
    }


}
