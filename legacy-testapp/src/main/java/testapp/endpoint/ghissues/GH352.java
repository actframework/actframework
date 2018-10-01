package testapp.endpoint.ghissues;

import act.controller.annotation.TemplateContext;
import act.controller.annotation.UrlContext;
import act.view.ViewManager;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;

/**
 * Verify fix to #352 and #424
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

    @GetAction("relative")
    public String testRelativePath(ViewManager viewManager) {
        return viewManager.getTemplate("test").render(context);
    }
}
