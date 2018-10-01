package testapp.endpoint;

import act.cli.Command;
import act.cli.JsonView;
import act.cli.Optional;
import act.cli.Required;
import act.util.PropertySpec;
import org.osgl.util.C;
import testapp.model.RGB;

import javax.enterprise.context.SessionScoped;
import java.util.List;
import java.util.Map;

/**
 * Test basic feature of CLI commander
 */
public class HelloCli {
    @Required(group = "foobar")
    @SessionScoped
    private String foo;

    @Optional(lead = "-c, --color")
    private RGB color;

    @Command("foobar")
    @JsonView
    public Map fooBar(@Required(group = "foobar") Integer bar) {
        return C.Map("foo", foo, "color", color, "bar", bar);
    }

}
