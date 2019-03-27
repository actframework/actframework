package ghissues.gh1069;

import act.controller.annotation.UrlContext;
import ghissues.BaseController;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;

@UrlContext("1069")
public class Gh1069_Service extends BaseController {
    @Inject
    Gh1069User.Dao dao;

    @PostAction
    public void test(Gh1069User user) {
        dao.save(user);
    }

}
