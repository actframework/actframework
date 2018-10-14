package testapp.endpoint.ghissues.gh446;

import act.app.App;
import act.controller.annotation.UrlContext;
import act.util.JsonView;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import testapp.endpoint.ghissues.GithubIssueBase;

@UrlContext("446")
public class GH446 extends GithubIssueBase {

    @Before
    public void before(App app) {
        app.getInstance(String.class);
    }

    @GetAction
    @JsonView
    public DataTable list(DataTable table) {
        return table;
    }

}
