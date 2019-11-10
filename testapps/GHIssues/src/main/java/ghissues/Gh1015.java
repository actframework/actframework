package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.PostAction;

import javax.persistence.Transient;
import javax.validation.constraints.*;

@UrlContext("1015")
public class Gh1015 extends BaseController {
    public static class User {

        @NotNull
        @Size(min = 3, max = 20)
        public String name;

        @Min(18)
        @Max(60)
        public Integer age = 0;

        @Transient
        public User invitor;
    }

    @PostAction
    public User create(User user) {
        return user;
    }
}
