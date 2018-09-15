package ghissues;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import act.db.jpa.JPADao;
import act.db.sql.tx.Transactional;
import act.util.PropertySpec;
import ghissues.gh823.Gh823User;
import org.osgl.$;
import org.osgl.mvc.annotation.*;

import javax.inject.Inject;

@UrlContext("823")
public class Gh823 extends BaseController {

    @Inject
    private JPADao<Integer, Gh823User> userDao;

    @GetAction("users")
    public Iterable<Gh823User> list() {
        return userDao.findAll();
    }

    @GetAction("users/{user}")
    public Gh823User get(@DbBind Gh823User user) {
        return user;
    }

    @PostAction("users")
    @PropertySpec("id")
    public Gh823User createUser(Gh823User user) {
        return userDao.save(user);
    }

    @PostAction("users2")
    @PropertySpec("id")
    @Transactional
    public Gh823User createUser2(Gh823User user) {
        return userDao.save(user);
    }

    @PutAction("users/{user}")
    public Gh823User update(@DbBind Gh823User user, Gh823User data) {
        $.merge(data).to(user);
        return userDao.save(user);
    }

    @PutAction("users2/{user}")
    @Transactional
    public Gh823User updateWithExplicitTransaction(@DbBind Gh823User user, Gh823User data) {
        $.merge(data).to(user);
        return userDao.save(user);
    }

}
