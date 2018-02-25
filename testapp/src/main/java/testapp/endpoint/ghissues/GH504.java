package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import act.inject.DefaultValue;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("504")
public class GH504 extends GithubIssueBase{

    @GetAction
    public int test(@DefaultValue("10") int n) {
        return n;
    }

    @GetAction("no_def")
    public int testNoDef(int n) {
        return n;
    }

}
