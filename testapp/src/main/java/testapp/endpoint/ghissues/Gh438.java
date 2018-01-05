package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("438")
public class Gh438 extends GithubIssueBase {

    @GetAction("a/foo:__foo__")
    public String testStyleA(String foo) {
        return foo;
    }

    @GetAction("b/{bar<__bar__>}")
    public String testStyleB(String bar) {
        return bar;
    }

}
