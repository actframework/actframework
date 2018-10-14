package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import org.osgl.mvc.annotation.PostAction;

import java.util.List;

@UrlContext("538")
public class GH538 extends GithubIssueBase{

    public static class Foo {
        public int id;
        public String name;
    }

    @PostAction()
    @JsonView
    public List<Foo> test(List<Foo> list) {
        return list;
    }

}
