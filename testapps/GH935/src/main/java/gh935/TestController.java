package gh935;

import act.inject.DefaultValue;
import org.osgl.mvc.annotation.GetAction;

public class TestController {

    @GetAction("/test/{test}")
    public String Test(@DefaultValue("Test") String test) {
        return test;
    }

}
