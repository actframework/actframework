package org.osgl.oms.controller.meta;

import org.osgl._;
import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.Catch;
import org.osgl.mvc.annotation.Finally;
import org.osgl.oms.controller.Interceptor;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * Aggregate interception meta info. This structure is used in
 * {@link org.osgl.oms.controller.meta.ControllerClassMetaInfo} and
 * {@link org.osgl.oms.controller.meta.ActionMethodMetaInfo}
 */
public class GroupInterceptorMetaInfo {

    private C.List<InterceptorMethodMetaInfo> beforeList = C.newList();
    private C.List<InterceptorMethodMetaInfo> afterList = C.newList();
    private C.List<InterceptorMethodMetaInfo> catchList = C.newList();
    private C.List<InterceptorMethodMetaInfo> finallyList = C.newList();

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

    public List<InterceptorMethodMetaInfo> beforeList() {
        return C.list(beforeList);
    }

    public List<InterceptorMethodMetaInfo> afterList() {
        return C.list(afterList);
    }

    @SuppressWarnings("unchecked")
    public List<CatchMethodMetaInfo> catchList() {
        return C.list((List) catchList);
    }

    public List<InterceptorMethodMetaInfo> finallyList() {
        return C.list(finallyList);
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
        beforeList = beforeList.sort();
        afterList = afterList.sort();
        catchList = catchList.sort();
        finallyList = finallyList.sort();
    }

    @Override
    public String toString() {
        StringBuilder sb = S.builder();
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

    private void appendList(String label, List<?> list, StringBuilder sb) {
        if (list.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(label).append("\n").append(S.join("\n", list));
    }

    private static enum _F {
        ;
        static _.Visitor<InterceptorMethodMetaInfo> mergeInto(final C.List<InterceptorMethodMetaInfo> toList, final String actionName) {
          return new _.Visitor<InterceptorMethodMetaInfo>() {
              @Override
              public void visit(InterceptorMethodMetaInfo info) throws _.Break {
                  info.mergeInto(toList, actionName);
              }
          };
        }
    }
}
