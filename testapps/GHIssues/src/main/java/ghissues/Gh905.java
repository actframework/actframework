package ghissues;

import act.controller.annotation.UrlContext;
import act.util.SimpleBean;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("905")
public class Gh905 extends BaseController {

    public static class Foo implements SimpleBean {
        public int id;
        public String name;
    }

    @PostAction
    public Foo create(Foo foo) {
        return foo;
    }

}
