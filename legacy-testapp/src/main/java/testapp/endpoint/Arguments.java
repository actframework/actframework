package testapp.endpoint;

import act.cli.Command;
import act.cli.Optional;
import act.util.JsonView;
import org.osgl.util.C;

import java.util.List;

public class Arguments {

    @Command("fixed")
    @JsonView
    public List<String> fixed(String s1, String s2) {
        return C.list(s1, s2);
    }

    @Command("varargs")
    @JsonView
    public List<String> varargs(String ... sa) {
        return C.listOf(sa);
    }

    @Command("mixed")
    @JsonView
    public List<String> mixed(String s1, String s2, String ... args) {
        return C.listOf(args).prepend(s2).prepend(s1);
    }

    @Command("mixed2")
    @JsonView
    public List<String> mixed2(String s1, @Optional String foo, String ... args) {
        return C.listOf(args).prepend(foo).prepend(s1);
    }
}
