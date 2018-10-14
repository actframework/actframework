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
    public static class Foo {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Integer id;

        public String name;
    }

    @Inject
    private JPADao<Integer, Foo> dao;

    @SessionVariable
    @DbBind
    private Foo foo;

    @PostAction
    public Foo create(final Foo foo) {
        return dao.save(foo);
    }

    @GetAction
    public Iterable<Foo> list() {
        return dao.findAll();
    }

    @PutAction("current/{target}")
    public Foo select(@DbBind Foo target, H.Session session) {
        session.put("foo", target.id);
        return target;
    }

    @GetAction("current")
    public Foo current() {
        return foo;
    }
}
