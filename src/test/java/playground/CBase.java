package playground;

import org.osgl.http.H;
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;

import java.util.Arrays;

public class CBase {

    private static class FakeResult extends Result {
        Object[] args;
        protected FakeResult(Object ... args) {
            super(H.Status.OK);
            this.args = args;
        }

        @Override
        public String toString() {
            return Arrays.toString(args);
        }
    }

    public Result render(Object ... args) {
        return new FakeResult(args);
    }

    public static Result ok() {
        return new Ok();
    }

    public static Result notFound(String message, Object... args) {
        return new NotFound(message, args);
    }

    public static Result badRequest(String reason, int code, int code2, Integer code3, Integer code4) {
        return new BadRequest(reason);
    }

    public static Result renderStatic(Object ... args) {
        return new FakeResult(args);
    }
}
