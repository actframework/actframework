package testapp.controller;

import org.osgl.mvc.annotation.*;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;
import act.app.AppContext;
import act.app.ActAppException;
import testapp.util.Trackable;

import static org.osgl.http.H.Method.*;

/**
 * A faked controller class with AppContext field
 */
@With({FilterA.class, FilterB.class, FilterC.class})
public class WithAppContext extends Trackable {
    private AppContext ctx;

    @Catch(ActAppException.class)
    public void handle(ActAppException e, AppContext ctx) {
        track("handle");
    }

    @Before
    public void setup() {
        track("setup");
    }

    @After
    public void after() {
        track("after");
    }

    @Finally
    public void teardown() {
        track("teardown");
    }

    @Action(value = "/no_ret_no_param", methods = {GET, PUT})
    public void noReturnNoParam() {
        track("noReturnNoParam");
    }

    @GetAction("/static_no_ret_no_param")
    public static String staticReturnStringNoParam() {
        return "foo";
    }

    public Result foo() {
        return new Ok();
    }

    public static void bar() {}
}
