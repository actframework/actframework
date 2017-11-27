package testapp.endpoint.ghissues;

import act.controller.annotation.TemplateContext;
import act.controller.annotation.UrlContext;
import act.view.ViewManager;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("352")
@TemplateContext("352")
public class GH352 extends GithubIssueBase {

    @Before
    public void setupRenderArgs() {
        context.renderArg("who", "Act");
    }

    @GetAction("inline")
    public String testInline(ViewManager viewManager) {
        return viewManager.getTemplate("Hello @who").render(context);
    }

    @GetAction
    public String test(ViewManager viewManager) {
        return viewManager.getTemplate("/gh/352/test.html").render(context);
    }
}
