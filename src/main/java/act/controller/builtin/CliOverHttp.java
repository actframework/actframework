package act.controller.builtin;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.app.ActionContext;
import act.cli.CliContext;
import act.cli.CliDispatcher;
import act.cli.CliOverHttpAuthority;
import act.cli.CliOverHttpContext;
import act.conf.AppConfig;
import act.controller.Controller;
import act.controller.annotation.Port;
import act.controller.annotation.UrlContext;
import act.handler.CliHandler;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.result.Result;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static act.controller.Controller.Util.*;

/**
 * Handles CLI over http requests
 */
@Deprecated
@UrlContext("cli")
@Port(AppConfig.PORT_CLI_OVER_HTTP)
public class CliOverHttp {

    private static final int MRU_LIMIT = 8;

    @Inject
    CliDispatcher dispatcher;

    @Inject
    H.Session session;

    @Inject
    AppConfig config;

    @Before
    public void before(AppConfig config) {
        CliOverHttpAuthority authority = config.cliOverHttpAuthority();
        authority.authorize();
    }

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
            mru = new ArrayList<>();
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
