package ghissues;

import act.controller.annotation.UrlContext;
import ghissues.gh820.BaseController;
import ghissues.gh820.IntegerService;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;

@UrlContext("820")
public class Gh820 extends BaseController<Integer, IntegerService> {

    @GetAction
    public List<Integer> get() {
        return service().produceList();
    }

}
