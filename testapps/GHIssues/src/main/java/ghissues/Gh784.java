package ghissues;

import act.data.annotation.Data;
import org.osgl.mvc.annotation.GetAction;

public class Gh784 extends BaseController {

    @Data
    public static class Foo {}

    @GetAction("784")
    public int test() {
        return new Foo().hashCode();
    }

}
