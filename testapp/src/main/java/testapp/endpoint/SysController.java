package testapp.endpoint;

import act.app.App;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;

public class SysController {

    @GetAction("/shutdown")
    public void shutdown() {
        System.out.print("received shutdown command...");
        App.instance().shutdown();
    }

    @GetAction("/ping")
    public Result ping() {
        return Ok.INSTANCE;
    }
}
