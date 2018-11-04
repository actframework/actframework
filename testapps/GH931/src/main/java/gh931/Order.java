package gh931;

import act.controller.annotation.UrlContext;
import act.db.morphia.MorphiaDao;
import act.db.morphia.MorphiaModel;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;

@Entity
public class Order extends MorphiaModel<Order> {
    public ObjectId accountId;
    public String product;
    public int quantity;

    public Account getAccount() {
        Account.Dao dao = Account.dao();
        return dao.findById(accountId);
    }

    @UrlContext("/orders")
    public static class Dao extends MorphiaDao<Order> {
        @GetAction
        public Iterable<Order> list() {
            List<Order> orders = findAllAsList();
            return orders;
        }
    }

}
