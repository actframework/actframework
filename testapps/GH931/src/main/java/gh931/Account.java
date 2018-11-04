package gh931;

import act.db.morphia.MorphiaDao;
import act.db.morphia.MorphiaModel;
import org.mongodb.morphia.annotations.Entity;

import javax.inject.Inject;

@Entity
public class Account extends MorphiaModel<Account> {

    public String name;

    public static class Dao extends MorphiaDao<Account> {
        @Inject
        private MorphiaDao<Order> orderDao;
    }
}
