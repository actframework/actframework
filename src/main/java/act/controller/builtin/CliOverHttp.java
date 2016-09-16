package act.controller.builtin;

import act.app.ActionContext;
import act.cli.CliContext;
import act.cli.CliDispatcher;
import act.cli.CliOverHttpContext;
import act.conf.AppConfig;
import act.controller.Controller;
import act.handler.CliHandler;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static act.controller.Controller.Util.*;

/**
 * Handles CLI over http requests
 */
@Controller(port = AppConfig.PORT_CLI_OVER_HTTP)
public class CliOverHttp {

    private static final int MRU_LIMIT = 8;

    @Inject
    CliDispatcher dispatcher;

    @Inject
    H.Session session;

    @Inject
    AppConfig config;

    @GetAction
    public Result home(AppConfig config) {
        String title = config.cliOverHttpTitle();
        boolean showSysCmd = config.cliOverHttpSysCmdEnabled();
        return render(dispatcher, mru(), title, showSysCmd);
    }

    @GetAction("cmd")
    public Result cmdPanel(String cmd) {
        CliHandler handler = handler(cmd);
        return render(handler, dispatcher, cmd);
    }

    @PostAction("cmd")
    public Result run(String cmd, ActionContext context) throws IOException {
        CliHandler handler = handler(cmd);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        CliContext cliContext = new CliOverHttpContext(context, os);
        handler.handle(cliContext);
        cliContext.flush();
        String txt = new String(os.toByteArray(), "UTF-8");
        txt = txt.replace("^J", "\n");
        return text(txt);
    }

    private List<String> mru() {
        List<String> mru = session.cached("cli_over_http_mru");
        if (null == mru) {
            mru = C.newList();
        }
        session.cache("cli_over_http_mru", mru, config.sessionTtl());
        return mru;
    }

    private void addToMru(String cmd) {
        List<String> mru = mru();
        mru.remove(cmd);
        mru.add(0, cmd);
        if (mru.size() > MRU_LIMIT) {
            mru.remove(mru.size() - 1);
        }
    }

    private CliHandler handler(String cmd) {
        CliHandler handler = dispatcher.handler(cmd);
        notFoundIfNull(handler);
        return handler;
    }


}
