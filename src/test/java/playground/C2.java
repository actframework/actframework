package playground;

import act.app.ActionContext;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.result.Result;

import javax.inject.Named;

public class C2 extends CBase {

    @Before(priority =  5, except = "doAnn")
    public void before() {
    }

    private boolean cond1() {
        return true;
    }

    public void foo(String id, String email, ActionContext ctx) {
        if (cond1()) {
            ctx.param("id", id);
            ctx.param("email", email);
            render(id, email);
        } else if (id.hashCode() < 10) {
            ctx.param("id", id);
            render(id);
        } else {
            throw render(email);
        }
    }

    public Result bar() {
        return ok();
    }

    public void foo(@Named("abc") String x, @Named("xyz") int i) {

    }
}
