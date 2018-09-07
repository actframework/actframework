package ghissues;

import act.controller.annotation.UrlContext;
import act.util.LogSupport;
import ghissues.gh820.Gh820Service;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;

@UrlContext("820")
public class Gh820 extends LogSupport {

    @Inject
    private Gh820Service<String> stringService;

    @Inject
    private Gh820Service<Integer> intService;

    @GetAction("string")
    public String getString() {
        return stringService.get();
    }

    @GetAction("int")
    public int getInt() {
        return intService.get();
    }

}
