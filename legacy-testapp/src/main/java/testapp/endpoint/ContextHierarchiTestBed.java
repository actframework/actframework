package testapp.endpoint;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("/cht")
public class ContextHierarchiTestBed {

    public static class IntermediateController extends ContextHierarchiTestBed {

    }

    public static class Endpoint extends IntermediateController {

        @GetAction("cht_test")
        public void test() {
        }

    }

}
