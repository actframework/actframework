package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("537")
public class GH537 extends GithubIssueBase{

    @GetAction("{nothing}")
    public String test(String nothing) {
        return nothing;
    }

}
