package ghissues.gh1069;

import act.db.jpa.JPADao;

import javax.persistence.*;

@Entity
public class Gh1069User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Unique(entity = Gh1069User.class, field = "name")
    public String name;

    public static class Dao extends JPADao<Integer, Gh1069User> {
    }


}