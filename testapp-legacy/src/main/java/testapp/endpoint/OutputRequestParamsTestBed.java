package testapp.endpoint;

import act.controller.annotation.UrlContext;
import act.event.EventBus;
import act.util.Output;
import act.util.OutputRequestParams;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("/output-params")
public class OutputRequestParamsTestBed {

    @Output
    public String field1;

    @GetAction("specific")
    public void testOutputSpecificParam(@Output String param1, int param2, EventBus param3) {
    }

    @GetAction("all")
    @OutputRequestParams
    public void testOutputAllParams(String param1, int param2, EventBus param3) {
    }

}
