package ghissues.gh1064;

import act.controller.annotation.UrlContext;
import ghissues.BaseController;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("1064")
public class Gh1064 extends BaseController {

    @PostAction("departments")
    public Department createDepartment(Department department) {
        return department;
    }

    @PostAction("users")
    public User createUser(User user) {
        return user;
    }

}
