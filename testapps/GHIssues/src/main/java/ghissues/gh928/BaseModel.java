package ghissues.gh928;

import act.util.SimpleBean;

import javax.persistence.*;

@MappedSuperclass
public abstract class BaseModel implements SimpleBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

}
