package ghissues;

import act.controller.annotation.UrlContext;
import act.util.PropertySpec;
import org.osgl.aaa.NoAuthentication;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.N;
import org.osgl.util.S;

@UrlContext("852")
@NoAuthentication
public class Gh852 {

    public static class Foo {
        public String id = S.random();
        public int number = N.randInt();
    }

    @GetAction
    @PropertySpec("-id")
    public Foo test() {
        return new Foo();
    }

}
