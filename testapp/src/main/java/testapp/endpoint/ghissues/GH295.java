package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("295")
public class GH295 extends GithubIssueBase {

    @GetAction("{foo_bar}")
    public String test(String foo_bar) {
        return foo_bar;
    }

}
