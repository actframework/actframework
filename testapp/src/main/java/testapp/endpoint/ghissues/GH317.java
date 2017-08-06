package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import act.db.morphia.MorphiaAdaptiveRecord;
import act.db.morphia.MorphiaDao;
import act.db.morphia.MorphiaDaoBase;
import act.db.morphia.MorphiaModelBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.osgl.mvc.annotation.PostAction;

import javax.validation.constraints.NotNull;

/**
 * Test Github #317
 */
@UrlContext("317")
public class GH317 extends GithubIssueBase {

    @Entity("gh317")
    public static class Order extends MorphiaAdaptiveRecord<Order> {

        public int quality;
        public Product product;

        public Order(Product product, int quality) {
            this.quality = quality;
            this.product = product;
        }

        public static class Dao extends MorphiaDao<Order> {
        }
    }

    @Entity("gh317_prod")
    public static class Product extends MorphiaModelBase<String, Product> {

        @Id
        public String name;

        public Product(String name) {
            this.name = name;
        }

        @Override
        public String _id() {
            return name;
        }

        @Override
        public Product _id(String id) {
            this.name = id;
            return this;
        }

        public static class Dao extends MorphiaDaoBase<String, Product> {
            @Override
            public Product findById(String id) {
                return new Product(id);
            }
        }
    }


    @PostAction
    public Order test(
            @DbBind("prod") @NotNull Product product,
            int quantity
    ) {
        return new Order(product, quantity);
    }

}
