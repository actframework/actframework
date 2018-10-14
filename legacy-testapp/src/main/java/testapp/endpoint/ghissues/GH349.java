package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.PostAction;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("349")
public class GH349 extends GithubIssueBase {

    @PostAction
    public void test() {
    }

}
