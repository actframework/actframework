package ghissues;

import act.controller.annotation.UrlContext;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("1026")
public class Gh1026 extends BaseController {

    public static class Foo {
        public Multimap<String, String> permissions = ArrayListMultimap.create();
    }

    @PostAction
    public Foo test(Foo foo) {
        return foo;
    }

    @GetAction
    public Foo get() {
        Foo foo = new Foo();
        foo.permissions.put("admin", "admin.create");
        foo.permissions.put("admin", "admin.update");
        foo.permissions.put("admin", "admin.delete");
        foo.permissions.put("user", "user.create");
        foo.permissions.put("user", "user.delete");
        return foo;
    }

}
