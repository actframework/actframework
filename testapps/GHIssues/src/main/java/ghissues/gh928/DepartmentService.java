package ghissues.gh928;

import act.controller.annotation.UrlContext;
import act.db.jpa.JPADao;
import ghissues.BaseController;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;

@UrlContext("928/departments")
public class DepartmentService extends BaseController {

    @Inject
    private JPADao<Integer, Department> dao;

    @GetAction
    public Iterable<Department> list() {
        return dao.findAll();
    }

}
