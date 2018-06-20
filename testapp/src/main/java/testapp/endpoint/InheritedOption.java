package testapp.endpoint;

import act.cli.Command;
import act.cli.JsonView;
import org.osgl.util.C;

import java.util.Map;

public class InheritedOption extends SingleOption {

    @Command("foo2")
    @JsonView
    public Map fooOnly() {
        return C.Map("foo", foo());
    }

}
