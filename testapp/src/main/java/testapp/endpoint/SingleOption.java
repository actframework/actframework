package testapp.endpoint;

import act.cli.Command;
import act.cli.Required;
import act.util.JsonView;
import org.osgl.util.C;

import java.util.Map;
import javax.enterprise.context.SessionScoped;

public class SingleOption {
    @Required
    @SessionScoped
    private String foo;

    @Command("foo")
    @JsonView
    public Map fooOnly() {
        return C.Map("foo", foo);
    }

    protected String foo() {
        return foo;
    }

}
