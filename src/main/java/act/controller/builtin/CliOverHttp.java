package act.controller.builtin;

import act.cli.CliDispatcher;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.meta.CommandParamMetaInfo;
import act.cli.meta.CommanderClassMetaInfo;
import act.conf.AppConfig;
import act.controller.Controller;
import act.handler.CliHandler;
import act.handler.builtin.cli.CliHandlerProxy;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;

import javax.inject.Inject;
import java.util.List;

import static act.controller.Controller.Util.notFoundIfNull;
import static act.controller.Controller.Util.render;

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
    public Result home() {
        return render(dispatcher, mru());
    }

    @GetAction("cmd")
    public Result cmdPanel(String cmd) {
        CliHandler handler = dispatcher.handler(cmd);
        notFoundIfNull(handler);

        CommandMethodMetaInfo methodMetaInfo = null;
        CommanderClassMetaInfo classMetaInfo = null;
        if (handler instanceof CliHandlerProxy) {
            CliHandlerProxy proxy = $.cast(handler);
            methodMetaInfo = proxy.methodMetaInfo();
            classMetaInfo = proxy.classMetaInfo();
            List<CommandParamMetaInfo> params = methodMetaInfo.params();
        }

        return render(handler, dispatcher, cmd, methodMetaInfo, classMetaInfo);
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


}
