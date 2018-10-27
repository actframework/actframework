package gh885;

import act.Act;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.annotation.GetAction;

public class Main {

    @Configuration("foo.bar")
    private String fooBar;

    @GetAction
    public String test() {
        return fooBar;
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }

}

