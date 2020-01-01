package ghissues;

import act.apidoc.SampleData;
import act.apidoc.SampleDataCategory;
import act.controller.annotation.UrlContext;
import act.util.Stateless;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Singleton;
import java.util.List;

@UrlContext("1262")
@Stateless
public class Gh1262 extends BaseController {

    public static class Address {
        public String street;
        public String postcode;
    }

    public static class Permission {
        public String name;
    }

    public static class Privilege {
        public int level;
    }

    public static class Role {
        public String name;
        public List<Permission> permissions;
        public Privilege privilege;
    }

    public static class User {
        @SampleData.Category(SampleDataCategory.USERNAME)
        public String name;
        public Address address;
        public List<Role> roles;
    }

    private User user = SampleData.generate(User.class, "user");

    @GetAction
    public User get() {
        return user;
    }

}
