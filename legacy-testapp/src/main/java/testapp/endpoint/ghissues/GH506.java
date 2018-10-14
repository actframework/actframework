package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import act.inject.SessionVariable;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("506")
public class GH506 extends GithubIssueBase{

    @Before
    public void setup(H.Session session) {
        if (!session.contains("n")) {
            session.put("n", 100);
        }
    }

    @GetAction
    public int test(@SessionVariable int n) {
        return n;
    }

}
