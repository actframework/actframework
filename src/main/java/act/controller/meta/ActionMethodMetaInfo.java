package act.controller.meta;

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
