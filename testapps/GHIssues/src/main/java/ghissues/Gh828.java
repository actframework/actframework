package ghissues;

import act.controller.annotation.UrlContext;
import act.util.LogSupport;
import org.osgl.mvc.annotation.PostAction;

import java.util.List;

@UrlContext("828")
public class Gh828 extends LogSupport {

    public static class Bar {
        public String name;
    }

    public static class Foo {
        public List<Bar> bars;
    }

    @PostAction
    public Foo[] test(Foo[] data) {
        return data;
    }

    @PostAction("list")
    public List<Foo> bindToList(List<Foo> data) {
        return data;
    }

}
