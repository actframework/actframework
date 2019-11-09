package ghissues;

import act.controller.annotation.UrlContext;
import act.util.SimpleBean;
import org.osgl.$;
import org.osgl.mvc.annotation.PostAction;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@UrlContext("1240")
public class Gh1240 extends BaseController {

    @Entity(name = "foo1240")
    @Table(name = "foo1240")
    public static class Foo implements SimpleBean {
        @Id
        public int id;

        public String name;
    }

    @PostAction
    public Foo create(Foo foo) {
        return foo;
    }

}
