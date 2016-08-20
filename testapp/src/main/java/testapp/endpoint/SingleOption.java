package testapp.endpoint;

import act.cli.Command;
import act.cli.JsonView;
import act.cli.Required;
import org.osgl.util.C;

import javax.enterprise.context.SessionScoped;
import java.util.Map;

public class SingleOption {
    @Required
    @SessionScoped
    private String foo;

    @Command("foo")
    @JsonView
    public Map fooOnly() {
        return C.map("foo", foo);
    }

    protected String foo() {
        return foo;
    }

}
