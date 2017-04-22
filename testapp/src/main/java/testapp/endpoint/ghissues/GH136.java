package testapp.endpoint.ghissues;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.With;

/**
 * Test `@With` on action methods
 */
public class GH136 {

    @With(GH136Interceptor.class)
    @GetAction("/gh/152/with")
    public String withInterceptor() {
        return "foo";
    }

    @GetAction("/gh/152/without")
    public String withoutInterceptor() {
        return "bar";
    }

}
