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

import act.inject.util.Sorter;
import act.util.LogSupportedDestroyableBase;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;

/**
 * Aggregate interception meta info. This structure is used in
 * {@link ControllerClassMetaInfo} and
 * {@link ActionMethodMetaInfo}
 */
@ApplicationScoped
public class GroupInterceptorMetaInfo extends LogSupportedDestroyableBase {

    private C.List<InterceptorMethodMetaInfo> beforeList = C.newList();
    private C.List<InterceptorMethodMetaInfo> afterList = C.newList();
    private C.List<InterceptorMethodMetaInfo> catchList = C.newList();
    private C.List<InterceptorMethodMetaInfo> finallyList = C.newList();

    public GroupInterceptorMetaInfo() {}

    public GroupInterceptorMetaInfo(GroupInterceptorMetaInfo copy) {
        mergeFrom(copy);
    }

    @Override
    protected void releaseResources() {
        destroyAll(beforeList, ApplicationScoped.class);
        destroyAll(afterList, ApplicationScoped.class);
        destroyAll(catchList, ApplicationScoped.class);
        destroyAll(finallyList, ApplicationScoped.class);
        beforeList.clear();
        afterList.clear();
        catchList.clear();
        finallyList.clear();
        super.releaseResources();
    }

    public void addBefore(InterceptorMethodMetaInfo before) {
        beforeList.add(before);
    }

    public void addAfter(InterceptorMethodMetaInfo after) {
        afterList.add(after);
    }

    public void addCatch(CatchMethodMetaInfo cat) {
        catchList.add(cat);
    }

    public void addFinally(InterceptorMethodMetaInfo after) {
        finallyList.add(after);
    }

    public void add(InterceptorMethodMetaInfo info, Class<? extends Annotation> interceptorType) {
        InterceptorType.of(interceptorType).addToGroup(info, this);
    }

    public void add(InterceptorMethodMetaInfo info, InterceptorType type) {
        type.addToGroup(info, this);
    }

    public C.List<InterceptorMethodMetaInfo> beforeList() {
        return C.list(beforeList);
    }

    public C.List<InterceptorMethodMetaInfo> afterList() {
        return C.list(afterList);
    }

    @SuppressWarnings("unchecked")
    public C.List<CatchMethodMetaInfo> catchList() {
        return C.list((List) catchList);
    }

    public C.List<InterceptorMethodMetaInfo> finallyList() {
        return C.list(finallyList);
    }

    private C.List<InterceptorMethodMetaInfo> allList() {
        return beforeList().lazy().append(afterList()).append((List<CatchMethodMetaInfo>)catchList()).append(finallyList());
    }

    public InterceptorMethodMetaInfo find(String methodName, String className) {
        for (InterceptorMethodMetaInfo info : allList()) {
            String infoMethodName = info.name();
            String infoClassName = info.classInfo().className();
            if (S.eq(methodName, infoMethodName) && S.eq(className, infoClassName)) {
                return info;
            }
        }
        return null;
    }

    public void mergeFrom(GroupInterceptorMetaInfo info, ControllerClassMetaInfo targetController) {
        mergeList(beforeList, targetController.convertDerived(info.beforeList));
        mergeList(afterList, targetController.convertDerived(info.afterList));
        mergeList(catchList, targetController.convertDerived(info.catchList));
        mergeList(finallyList, targetController.convertDerived(info.finallyList));
    }

    public void mergeFrom(GroupInterceptorMetaInfo info) {
        mergeList(beforeList, info.beforeList);
        mergeList(afterList, info.afterList);
        mergeList(catchList, info.catchList);
        mergeList(finallyList, info.finallyList);
    }

    public void mergeFrom(GroupInterceptorMetaInfo classInterceptors, String actionName) {
        classInterceptors.beforeList.each(_F.mergeInto(this.beforeList, actionName));
        classInterceptors.afterList.each(_F.mergeInto(this.afterList, actionName));
        classInterceptors.catchList.each(_F.mergeInto(this.catchList, actionName));
        classInterceptors.finallyList.each(_F.mergeInto(this.finallyList, actionName));
        Sorter.sort(beforeList);
        Sorter.sort(afterList);
        Sorter.sort(catchList);
        Sorter.sort(finallyList);
    }

    @Override
    public String toString() {
        S.Buffer sb = S.buffer();
        appendList("BEFORE", beforeList, sb);
        appendList("AFTER", afterList, sb);
        appendList("CATCH", catchList, sb);
        appendList("FINALLY", finallyList, sb);
        return sb.toString();
    }

    private static <T> void mergeList(List<? super T> toList, List<? extends T> fromList) {
        for (T t : fromList) {
            if (!toList.contains(t)) {
                toList.add(t);
            }
        }
    }

    private void appendList(String label, List<?> list, S.Buffer sb) {
        if (list.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(label).append("\n").append(S.join("\n", list));
    }

    private enum _F {
        ;

        static $.Visitor<InterceptorMethodMetaInfo> mergeInto(final C.List<InterceptorMethodMetaInfo> toList, final String actionName) {
            return new $.Visitor<InterceptorMethodMetaInfo>() {
                @Override
                public void visit(InterceptorMethodMetaInfo info) throws $.Break {
                    info.mergeInto(toList, actionName);
                }
            };
        }
    }
}
