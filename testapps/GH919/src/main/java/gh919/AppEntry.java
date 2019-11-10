package gh919;

import act.Act;
import act.controller.annotation.UrlContext;
import act.data.Sensitive;
import act.util.SimpleBean;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("919")
public class AppEntry {

    public static class Foo implements SimpleBean {
        @Sensitive
        public String name;
    }

    @PostAction
    public Foo post() {
        return new Foo();
    }

    @GetAction
    public String get() {
        return "Hello";
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }
}
