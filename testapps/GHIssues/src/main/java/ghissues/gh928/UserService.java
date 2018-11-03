package ghissues.gh928;

import act.controller.annotation.UrlContext;
import act.db.jpa.JPADao;
import ghissues.BaseController;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;

@UrlContext("928/users")
public class UserService extends BaseController {

    @Inject
    private JPADao<Integer, User> dao;

    @GetAction
    public Iterable<User> list() {
        return dao.findAll();
    }

}
