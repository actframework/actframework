package act.handler;

import org.osgl.http.H;
import org.osgl.mvc.result.MethodNotAllowed;
import org.osgl.mvc.result.Result;

import java.io.Serializable;

public abstract class UnknownHttpMethodProcessor implements Serializable {

    public static UnknownHttpMethodProcessor METHOD_NOT_ALLOWED = new NotAllowed();

    public static UnknownHttpMethodProcessor NOT_IMPLEMENTED = new NotImplemented();

    public abstract Result handle(H.Method method);

    private static class NotAllowed extends UnknownHttpMethodProcessor {
        @Override
        public Result handle(H.Method method) {
            return MethodNotAllowed.INSTANCE;
        }

        private Object readResolve() {
            return METHOD_NOT_ALLOWED;
        }
    }

    private static class NotImplemented extends UnknownHttpMethodProcessor {
        @Override
        public Result handle(H.Method method) {
            return org.osgl.mvc.result.NotImplemented.INSTANCE;
        }
        private Object readResolve() {
            return NOT_IMPLEMENTED;
        }
    }
}
