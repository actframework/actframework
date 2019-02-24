package ghissues.gh1064;

import act.data.annotation.Data;
import act.util.SimpleBean;

import java.util.List;

@Data
public class Department implements SimpleBean {

    public Long id;

    public String name;

    public Department parent;

    public List<Department> children;

    public List<User> users;

}
