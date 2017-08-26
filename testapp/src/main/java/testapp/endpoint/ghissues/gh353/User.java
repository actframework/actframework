package testapp.endpoint.ghissues.gh353;

import act.db.morphia.MorphiaAdaptiveRecord;
import act.db.morphia.MorphiaDao;
import org.mongodb.morphia.annotations.Entity;

@Entity
public class User extends MorphiaAdaptiveRecord<User> {
    public String name;

    public User() {}

    public User(String name) {
        this.name = name;
    }

    public static class Dao extends MorphiaDao<User> {}
}
