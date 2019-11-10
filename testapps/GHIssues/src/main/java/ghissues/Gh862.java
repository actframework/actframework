package ghissues;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import act.db.jpa.JPADao;
import act.inject.SessionVariable;
import org.osgl.http.H;
import org.osgl.mvc.annotation.*;

import javax.inject.Inject;
import javax.persistence.*;

@UrlContext("862")
public class Gh862 extends BaseController {

    @Entity(name = "foo862")
    public static class Gh862Foo {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Integer id;

        public String name;
    }

    @Inject
    private JPADao<Integer, Gh862Foo> dao;

    @SessionVariable
    @DbBind
    private Gh862Foo foo;

    @PostAction
    public Gh862Foo create(final Gh862Foo foo) {
        return dao.save(foo);
    }

    @GetAction
    public Iterable<Gh862Foo> list() {
        return dao.findAll();
    }


    @PutAction("current/{target}")
    public Gh862Foo select(@DbBind Gh862Foo target, H.Session session) {
        session.put("foo", target.id);
        return target;
    }

    @GetAction("current")
    public Gh862Foo current() {
        return foo;
    }
}
