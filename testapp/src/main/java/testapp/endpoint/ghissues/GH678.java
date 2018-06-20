package testapp.endpoint.ghissues;

import org.osgl.mvc.annotation.GetAction;

public class GH678 extends GithubIssueBase {

    @GetAction("678")
    public void test() {
        renderText("").addHeader("H678", "678");
    }

}
