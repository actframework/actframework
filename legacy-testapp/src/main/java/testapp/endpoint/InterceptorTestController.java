package testapp.endpoint;

import act.app.ActionContext;
import act.controller.Controller;
import act.controller.annotation.UrlContext;
import org.osgl.exception.UnexpectedException;
import org.osgl.mvc.annotation.*;
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.Result;

@UrlContext("/aop")
@SuppressWarnings("unused")
public class InterceptorTestController extends Controller.Util {

    @Catch(Exception.class)
    public Result handleException(Exception e) {
        return text("bar-" + e.getMessage());
    }

    @Before
    public void validate(int n) {
        if (n < 0) {
            throw new BadRequest();
        }
    }

    @GetAction("foo")
    public int foo(int n) {
        return n;
    }

    @GetAction("bar")
    public int bar(int n) throws Exception {
        if (n % 2 == 0) {
            throw new ArrayIndexOutOfBoundsException("array:" + n);
        } else if (n % 3 == 0) {
            throw new UnexpectedException("unexpected:%s", n);
        } else {
            throw new Exception("exception:" + n);
        }
    }

    @Catch(UnexpectedException.class)
    public Result handleUnexpectedException(UnexpectedException e) {
        return text(e.getMessage());
    }

    @Catch(ArrayIndexOutOfBoundsException.class)
    public Result handleArrayIndexOutOfBoundsException(ArrayIndexOutOfBoundsException e) {
        return text(e.getMessage());
    }

    @After
    public void decorate(Result result, ActionContext context) {
        context.resp().addHeader("foo-code", String.valueOf(result.statusCode()));
    }

}
