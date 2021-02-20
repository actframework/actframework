package ghissues.gh1378;

import act.data.annotation.Data;
import act.db.DB;
import act.util.SimpleBean;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@DB("ebean")
public class Category implements SimpleBean {
    @Id
    public int id;
    public String name;
}
