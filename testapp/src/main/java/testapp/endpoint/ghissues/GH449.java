package testapp.endpoint.ghissues;

import act.app.App;
import act.controller.annotation.UrlContext;
import act.util.SimpleBean;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("449")
public class GH449 extends GithubIssueBase{

    public static class Data implements SimpleBean {
        public String name;
    }

    @Before
    public void someStupidLogic(App app) {
        String empty = app.getInstance(String.class);
    }

    @GetAction
    public String test(Data data) {
        return data.name;
    }

}
