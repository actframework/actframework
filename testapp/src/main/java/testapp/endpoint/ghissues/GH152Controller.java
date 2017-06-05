package testapp.endpoint.ghissues;

import org.osgl.mvc.annotation.GetAction;

public class GH152Controller extends GithubIssueBase {

    @GetAction("152")
    public String gh152() {
        return "gh152";
    }

    @GetAction("152/method")
    public String gh152Method() {
        return "gh152Method";
    }

}
