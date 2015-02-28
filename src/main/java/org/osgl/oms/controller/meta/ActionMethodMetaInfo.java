package org.osgl.oms.controller.meta;

import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class ActionMethodMetaInfo extends ActionMethodMetaInfoBase<ActionMethodMetaInfo> {
    private GroupInterceptorMetaInfo interceptors = new GroupInterceptorMetaInfo();

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
        StringBuilder sb = S.builder(super.toString())
                .append("\n").append(interceptors);
        return sb.toString();
    }
}
