package testapp.controller;

import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;
import testapp.util.Trackable;

public class Controller extends Trackable {

    public static Result render(Object ... args) {
        return new FakeResult(args);
    }

    public static Result ok() {
        return Ok.INSTANCE;
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
