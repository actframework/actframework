package testapp.endpoint;

import act.controller.annotation.UrlContext;
import act.handler.PreventDoubleSubmission;
import org.osgl.mvc.annotation.PostAction;

// Test double submission protection
@UrlContext("/dsp")
public class DspTestBed {

    @PreventDoubleSubmission
    @PostAction
    public void foo() {
    }

}
