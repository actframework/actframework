package testapp.endpoint.ghissues;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.With;

/**
 * Test `@With` on action methods
 */
public class GH136 extends GithubIssueBase {

    @With(GH136Interceptor.class)
    @GetAction("136/with")
    public String withInterceptor() {
        return "foo";
    }

    @GetAction("136/without")
    public String withoutInterceptor() {
        return "bar";
    }

}
