package act.controller.meta;

import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

/**
 * Unlike other interceptors (Before/After/Finally), Catch interceptor
 * has a special attribute: value, the exception class. Only when the
 * exception thrown out is instance of the class or subclass of the class,
 * the catch interceptor will be executed
 */
public class CatchMethodMetaInfo extends InterceptorMethodMetaInfo {
    private static final List<String> CATCH_THROWABLE = C.list(Throwable.class.getName());
    private List<String> targetExceptionClassNames = CATCH_THROWABLE;

    protected CatchMethodMetaInfo(CatchMethodMetaInfo copy, ControllerClassMetaInfo clsInfo) {
        super(copy, clsInfo);
        this.targetExceptionClassNames = copy.targetExceptionClassNames;
    }

    public CatchMethodMetaInfo(ControllerClassMetaInfo clsInfo) {
        super(clsInfo);
    }

    @Override
    protected void releaseResources() {
        targetExceptionClassNames = null;
        super.releaseResources();
    }

    public CatchMethodMetaInfo exceptionClasses(List<String> list) {
        targetExceptionClassNames = C.list(list);
        return this;
    }

    public List<String> exceptionClasses() {
        return targetExceptionClassNames;
    }

    @Override
    public String toString() {
        return toStrBuffer(S.newBuffer()).toString();
    }

    @Override
    protected S.Buffer toStrBuffer(S.Buffer sb) {
        StringBuilder prependix = S.builder("catch").append(targetExceptionClassNames).append(" ");
        return super.toStrBuffer(sb).prepend(prependix);
    }

    @Override
    protected InterceptorMethodMetaInfo doExtend(ControllerClassMetaInfo clsInfo) {
        return new CatchMethodMetaInfo(this, clsInfo);
    }
}
