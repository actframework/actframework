package ghissues.gh823;

import act.util.SimpleBean;

import javax.persistence.*;

@Entity(name = "user_823")
public class Gh823User implements SimpleBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String name;
}
