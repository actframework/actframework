package ghissues;

import act.controller.annotation.UrlContext;
import act.data.annotation.Data;
import act.util.PropertySpec;
import act.util.SimpleBean;
import org.osgl.aaa.NoAuthentication;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

@UrlContext("1118")
@NoAuthentication
public class Gh1118 {

    public static class Employee {
        public String email = "jen@abc.com";
    }

    @Data
    public static class Bar implements SimpleBean {
        public int id = 3;
        public Employee employee = new Employee();
    }

    @Data
    public static class Foo implements SimpleBean {
        public static final String TAG = "tag";
        public String name = "foo";
        public int count = 110;
        public Bar bar = new Bar();
    }

    @GetAction
    @PropertySpec("count, bar")
    public Iterable<Foo> test() {
        return C.list(new Foo());
    }
}
