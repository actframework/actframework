package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;

/**
 * Test `@With` on action methods
 */
@UrlContext("631")
public class GH631 extends GithubIssueBase {

    @GetAction("{header}")
    public String test(String header, H.Request req) {
        return req.header(header);
    }

}
