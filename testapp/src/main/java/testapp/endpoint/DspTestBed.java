package testapp.endpoint;

import act.controller.Controller;
import act.handler.PreventDoubleSubmission;
import org.osgl.mvc.annotation.PostAction;

// Test double submission protection
@Controller("/dsp")
public class DspTestBed {

    @PreventDoubleSubmission
    @PostAction
    public void foo() {
    }

}
