package ghissues.gh976;

import act.db.jpa.JPADao;
import act.util.SimpleBean;

import javax.persistence.*;

@Entity(name = "foo976")
public class Foo implements SimpleBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String name;

    public static class Dao extends JPADao<Integer, Foo> {
    }

}
