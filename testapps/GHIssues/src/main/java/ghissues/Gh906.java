package ghissues;

import act.controller.annotation.UrlContext;
import act.util.SimpleBean;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.ResponseContentType;
import org.osgl.util.S;

public class Gh906 {

    public static class Foo implements SimpleBean {
        public int id;

        public Foo(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return S.string(id);
        }
    }

    public abstract static class Super extends BaseController {
        @GetAction
        @ResponseContentType(H.MediaType.TXT)
        public Foo getFoo(int n) {
            return new Foo(n);
        }
    }

    @UrlContext("906")
    public static class Child extends Super {}

}
