package act.controller.builtin;

import act.Act;
import act.app.ActionContext;
import act.cli.CliDispatcher;
import act.conf.AppConfig;
import act.controller.Controller;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Inject;
import java.util.List;

import static act.controller.Controller.Util.render;

/**
 * Handles CLI over http requests
 */
@Controller(port = AppConfig.PORT_CLI_OVER_HTTP)
public class CliOverHttp {

    @Inject
    CliDispatcher dispatcher;

    @Inject
    H.Session session;

    @Inject
    AppConfig config;

    @GetAction
    public Result home() {
        return render(dispatcher, mru());
    }


    private List<String> mru() {
        List<String> mru = session.cached("cli_over_http_mru");
        if (null == mru) {
            mru = C.newList();
        }
        session.cache("cli_over_http_mru", mru, config.sessionTtl());
        return mru;
    }

}
