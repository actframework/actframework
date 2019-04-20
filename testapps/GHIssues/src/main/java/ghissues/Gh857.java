package ghissues;

import act.annotations.Order;
import act.annotations.Sorted;
import act.controller.annotation.UrlContext;
import org.osgl.$;
import org.osgl.inject.annotation.TypeOf;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

import java.util.List;

@UrlContext("857")
public class Gh857 extends BaseController {

    public interface Service857 {
        String name();
    }

    @Order(2)
    public static class BarService implements Service857 {
        @Override
        public String name() {
            return "bar";
        }
    }

    @Order(11)
    public static class FooService implements Service857 {
        @Override
        public String name() {
            return "foo";
        }

    }

    @Order(3)
    public static class ZooService implements Service857 {
        @Override
        public String name() {
            return "zoo";
        }
    }


    @TypeOf
    @Sorted
    private List<Service857> services;

    @GetAction
    public List<String> test() {
        return C.list(services).map(new $.Transformer<Service857, String>() {
            @Override
            public String transform(Service857 service857) {
                return service857.name();
            }
        });
    }

}
