package testapp.endpoint.ghissues;

import act.controller.annotation.TemplateContext;
import act.controller.annotation.UrlContext;
import act.view.ProvidesImplicitTemplateVariable;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("354")
@TemplateContext("354")
public class GH354 extends GithubIssueBase {

    @ProvidesImplicitTemplateVariable
    public static int bar354() {
        return 3;
    }

    @ProvidesImplicitTemplateVariable("reqUrl")
    public static String url(H.Request request) {
        return request.url();
    }

    @GetAction
    public void foo() {
        render();
    }

}
