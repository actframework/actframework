package ghissues;

import act.cli.Command;
import act.controller.annotation.UrlContext;
import act.data.annotation.Data;
import act.util.PropertySpec;
import act.util.SimpleBean;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

@UrlContext("1117")
public class Gh1117 extends BaseController {

    @Data
    public static class Foo implements SimpleBean {
        public static final String TAG = "tag";
        public String name = "foo";
        public String bar = "bar";
    }

    /**
     * Note the issue only occur when executing CLI command and it has to be manually verified.
     */
    @GetAction
    @PropertySpec("-bar")
    @Command("gh1117")
    public Iterable<Foo> test() {
        return C.list(new Foo());
    }
}
