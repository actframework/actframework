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

import org.osgl.util.S;

import java.util.List;

public class ActionMethodMetaInfo extends HandlerMethodMetaInfo<ActionMethodMetaInfo> {
    private GroupInterceptorMetaInfo interceptors = new GroupInterceptorMetaInfo();

    public ActionMethodMetaInfo(ControllerClassMetaInfo classMetaInfo) {
        super(classMetaInfo);
    }

    public ActionMethodMetaInfo(ActionMethodMetaInfo parentAction, ControllerClassMetaInfo thisClass) {
        super(parentAction, thisClass);
    }

    @Override
    protected void releaseResources() {
        interceptors.destroy();
        super.releaseResources();
    }

    public ActionMethodMetaInfo mergeFromClassInterceptors(GroupInterceptorMetaInfo info) {
        interceptors.mergeFrom(info, name());
        return this;
    }

    public List<InterceptorMethodMetaInfo> beforeList() {
        return interceptors.beforeList();
    }

    public List<InterceptorMethodMetaInfo> afterList() {
        return interceptors.afterList();
    }

    public List<CatchMethodMetaInfo> catchList() {
        return interceptors.catchList();
    }

    public List<InterceptorMethodMetaInfo> finallyList() {
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
}
