package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.ResponseStatus;
import org.osgl.util.C;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("232")
public class GH232 extends GithubIssueBase {

    private static final int STATUS_CREATED = 202;

    @GetAction("foo")
    @ResponseStatus(STATUS_CREATED)
    public void foo() {
    }

    @GetAction("{s}")
    @ResponseStatus(STATUS_CREATED)
    public String test(String s) {
        return s;
    }

    @GetAction("map/{s}")
    @ResponseStatus(STATUS_CREATED)
    public Object testMap(String s) {
        return C.Map("s", s);
    }

}
