package testapp.endpoint;

import act.Act;
import act.app.App;
import org.osgl.inject.annotation.Provided;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;

public class SysController {

    @GetAction("/shutdown")
    public void shutdown(final @Provided App app) {
        app.jobManager().now(new Runnable() {
            @Override
            public void run() {
                Act.shutdownApp(app);
            }
        });
    }

    @GetAction("/ping")
    public Result ping() {
        return Ok.INSTANCE;
    }
}
