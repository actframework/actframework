package ghissues;

import act.data.annotation.Data;
import act.util.AdaptiveBean;
import org.osgl.mvc.annotation.GetAction;

public class Gh785 {

    @Data
    public static class Foo extends AdaptiveBean {}

    @GetAction("785")
    public int test() {
        Foo f1 = new Foo();
        f1.putValue("id", 123);
        Foo f2 = new Foo();
        f2.putValue("id", 246);
        return f1.hashCode() - f2.hashCode();
    }

}
