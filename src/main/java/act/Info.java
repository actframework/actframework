package act;

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
import act.app.App;
import act.cli.Command;
import act.handler.NonBlock;
import act.inject.param.NoBind;
import act.sys.Env;
import act.util.Banner;
import act.util.JsonView;
import com.alibaba.fastjson.JSON;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

import static act.util.ByteBuffers.wrap;

@Singleton
@SuppressWarnings("unused")
public class Info {

    // a unit of information data
    @NoBind
    public static class Unit {
        private static final String CNT_TXT = H.Format.TXT.contentType();
        private static final String CNT_JSON = H.Format.JSON.contentType();
        ByteBuffer json;
        ByteBuffer txt;

        public Unit(Map<String, Object> data) {
            this(data, JSON.toJSONString(data));
        }

        public Unit(Map<String, Object> data, String txt) {
            this.json = wrap(JSON.toJSONString(data));
            this.txt = wrap(txt);
        }

        public Unit(String name, Object val) {
            this.json = wrap(JSON.toJSONString(C.Map(name, val)));
            this.txt = wrap(S.string(val));
        }

        public void applyTo(ActionContext context) {
            H.Response resp = context.resp();
            if (context.acceptJson()) {
                resp.contentType(CNT_JSON);
                resp.writeContent(json.duplicate());
            } else {
                resp.contentType(CNT_TXT);
                resp.writeContent(txt.duplicate());
            }
        }
    }

    @NoBind
    private Map<String, Object> versionData;
    private Unit info;
    private Unit pid;
    private Unit version;

    @Inject
    public Info(App app) {
        init(app);
    }

    @GetAction("info")
    @NonBlock
    public void show(ActionContext context) {
        info.applyTo(context);
    }

    @GetAction("pid")
    @NonBlock
    public void pid(ActionContext context) {
        pid.applyTo(context);
    }

    @NonBlock
    @JsonView
    @GetAction("version")
    public void version(ActionContext context) {
        version.applyTo(context);
    }

    @Command(name = "act.version, act.ver", help = "Print app/actframework version")
    public Map<String, Object> getVersions() {
        return versionData;
    }

    private void init(App app) {
        initPidBuffer();
        initInfoBuffer(app);
        initVersion();
    }

    private void initPidBuffer() {
        this.pid = new Unit("pid", Env.PID.get());
    }

    private void initInfoBuffer(App app) {
        Map<String, Object> map = C.Map(
                "actVersion", Act.VERSION.getVersion(),
                "appVersion", Act.appVersion().getVersion(),
                "appName", app.name(),
                "pid", Env.PID.get(),
                "baseDir", app.base().getAbsoluteFile(),
                "mode", Act.mode().name(),
                "profile", Act.profile(),
                "group", Act.nodeGroup()
        );
        this.info = new Unit(map, Banner.cachedBanner());
    }

    private void initVersion() {
        versionData = C.Map("act", Act.VERSION, "app", Act.appVersion());
        version = new Unit(versionData);
    }


}
