package ghissues;

import act.controller.annotation.UrlContext;
import act.util.PropertySpec;
import act.util.SimpleBean;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.ResponseContentType;
import org.osgl.util.S;

public class Gh906 {

    public static class Foo implements SimpleBean {
        public int id;
        public String name;

        public Foo(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return S.string(id);
        }
    }

    public abstract static class Super extends BaseController {
        @GetAction
        @ResponseContentType(H.MediaType.TXT)
        @PropertySpec("id")
        public Foo getFoo(int id, String name) {
            return new Foo(id, name);
        }
    }

    @UrlContext("906")
    public static class Child extends Super {}

}
