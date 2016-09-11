package testapp.endpoint.enhancement;

import act.view.ActServerError;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

/**
 * Test A Controller action method enhancement failure
 * found on 11-Sep-2016
 */
public class Controler20160911 {

    @GetAction("/issue/enhancer/20160911")
    public Result test(int status) {
        return ActServerError.of(status);
    }

}
