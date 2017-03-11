package testapp.endpoint;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;

@Controller("/cht")
public class ContextHierarchiTestBed {

    public static class IntermediateController extends ContextHierarchiTestBed {

    }

    public static class Endpoint extends IntermediateController {

        @GetAction("cht_test")
        public void test() {
        }

    }

}
