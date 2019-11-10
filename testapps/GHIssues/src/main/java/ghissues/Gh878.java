package ghissues;

import act.controller.annotation.UrlContext;
import act.util.PropertySpec;
import act.util.SimpleBean;
import org.osgl.aaa.NoAuthentication;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.*;

@UrlContext("878")
@NoAuthentication
public class Gh878 {

    public static class Foo implements SimpleBean{
        public Bar bar;
        public Integer id;

        public Foo(Bar bar, Integer id) {
            this.bar = bar;
            this.id = id;
        }
    }

    public static class Bar implements SimpleBean {
        public Integer id;
        public String name;

        public Bar(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @GetAction("list")
    @PropertySpec("-bar.id")
    public Iterable<Foo> test() {
        return C.list(new Foo(new Bar(1, "a"), 1), new Foo(new Bar(2, "b"), 2));
    }

    @GetAction
    @PropertySpec("-bar.id,-id")
    public Foo test2() {
        return new Foo(new Bar(1, "a"), 1);
    }

}
