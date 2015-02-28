package playground;

import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.NoResult;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.controller.RequestDispatcher;
import org.osgl.util.E;

public class XReqDispatcher extends RequestDispatcher {
    @Override
    public Result handle(AppContext appContext) {
        C1 c1 = new C1();
        String s1 = appContext.param("x");
        return c1.bar(s1);
    }

    public Result handle2(AppContext appContext) {
        String id = appContext.param("id");
        String email = new EmailBinder().resolve("email", appContext.req());
        boolean b = StringValueResolver.predefined(boolean.class).resolve(appContext.param("b"));
        try {
            C1.doIt(id, appContext, email, b);
            throw E.unexpected("result not captured");
        } catch (Result r) {
            return r;
        }
    }

}
