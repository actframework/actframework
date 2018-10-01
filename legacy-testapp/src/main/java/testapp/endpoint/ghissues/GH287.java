package testapp.endpoint.ghissues;

import act.controller.annotation.TemplateContext;
import act.controller.annotation.UrlContext;
import act.view.ProvidesImplicitTemplateVariable;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

import java.util.List;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("287")
@TemplateContext("287")
public class GH287 extends GithubIssueBase {

    @ProvidesImplicitTemplateVariable
    public List<Integer> bar() {
        return C.list(1, 2, 3);
    }

    @GetAction
    public void foo() {
        throw render();
    }

}
