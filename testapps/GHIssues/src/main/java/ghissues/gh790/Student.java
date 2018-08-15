package ghissues.gh790;

import act.data.annotation.Data;
import act.util.SimpleBean;

@Data
public class Student implements SimpleBean {
    public String firstName;
    public String lastName;
    public String grade;
}
