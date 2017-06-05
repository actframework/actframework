package testapp.endpoint.ghissues;

import org.osgl.mvc.annotation.GetAction;

/**
 * Test `@With` on action methods
 */
public class GH229 extends GithubIssueBase {

    @GetAction("229/{s}")
    public String test(String s) {
        return s;
    }

}
