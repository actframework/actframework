package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("636")
public class GH636 extends GithubIssueBase {

    @Configuration("gh636.conf")
    private String conf;

    @GetAction
    public String test() {
        return conf;
    }

}
