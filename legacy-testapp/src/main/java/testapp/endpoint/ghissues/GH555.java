package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.ResponseContentType;

@UrlContext("555")
public class GH555 extends GithubIssueBase {

    @GetAction
    @ResponseContentType(H.MediaType.JAVASCRIPT)
    public String test() {
        return "alert('Hello World!')";
    }

}
