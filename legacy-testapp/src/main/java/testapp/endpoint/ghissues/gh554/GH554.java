package testapp.endpoint.ghissues.gh554;


import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import testapp.endpoint.ghissues.GithubIssueBase;

@UrlContext("554")
public class GH554 extends GithubIssueBase  {

    @GetAction
    public void test(String id) {
        EventListener.excute(new MsgTemplate(id));
    }

}
