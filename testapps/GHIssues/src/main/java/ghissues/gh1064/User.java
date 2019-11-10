package ghissues.gh1064;

import act.data.annotation.Data;
import act.util.SimpleBean;

@Data
public class User implements SimpleBean {

    public Long id;


    public Department department;
}
