package ghissues;

import act.controller.annotation.UrlContext;
import ghissues.gh820.Gh820BaseController;
import ghissues.gh820.IntegerService;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;

@UrlContext("820")
public class Gh820 extends Gh820BaseController<Integer, IntegerService> {

    @GetAction
    public List<Integer> get() {
        return service().produceList();
    }

}
