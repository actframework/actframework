package testapp.endpoint.ghissues.gh417;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import org.osgl.mvc.annotation.GetAction;
import testapp.endpoint.ghissues.GithubIssueBase;

@UrlContext("417")
public class GH417 extends GithubIssueBase {

    @JsonView
    @GetAction
    public Record test() {
        return new Record();
    }

}
