package ghissues.gh1072;

import act.controller.annotation.UrlContext;
import act.db.jpa.JPADao;
import act.util.SimpleBean;

import javax.persistence.*;

@MappedSuperclass
public class BaseModel implements SimpleBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    @UrlContext("1072")
    public static class Dao<M extends BaseModel> extends JPADao<Long, M> {
    }
}
