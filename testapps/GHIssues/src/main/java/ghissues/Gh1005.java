package ghissues;

import act.controller.annotation.UrlContext;
import act.util.PropertySpec;
import org.osgl.aaa.NoAuthentication;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("1005")
@NoAuthentication
public class Gh1005 {

    public static class Foo {
        public int id;
        public boolean flag;
        public String name;
    }

    @PostAction
    public Foo create(Foo foo, String fields) {
        PropertySpec.current.set(fields);
        return foo;
    }

}
