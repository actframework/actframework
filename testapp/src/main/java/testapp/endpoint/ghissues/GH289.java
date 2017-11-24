package testapp.endpoint.ghissues;

import act.controller.annotation.TemplateContext;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("289")
@TemplateContext("289")
public class GH289 extends GithubIssueBase {

    @GetAction
    public void test() {
        String who = "World";
        render("Hello @who!", who);
    }

}
