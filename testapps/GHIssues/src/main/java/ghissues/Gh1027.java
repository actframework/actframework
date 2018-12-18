package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.PostAction;

import java.util.Map;

@UrlContext("1027")
public class Gh1027 extends BaseController {

    public static class Foo {
        public String name;
    }

    @PostAction
    public Map<String, Foo> test(Map<String, Foo> data) {
        return data;
    }

}
