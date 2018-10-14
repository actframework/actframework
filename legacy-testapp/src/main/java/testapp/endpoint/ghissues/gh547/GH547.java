package testapp.endpoint.ghissues.gh547;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import org.osgl.mvc.annotation.PostAction;
import testapp.endpoint.ghissues.GithubIssueBase;

@UrlContext("547")
public class GH547 extends GithubIssueBase  {

    @PostAction
    @JsonView
    public DataTable test(DataTable table) {
        return table;
    }

}
