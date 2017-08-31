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
import act.sys.Env;
import act.util.Banner;
import org.osgl.mvc.annotation.GetAction;

import static act.controller.Controller.Util.jsonMap;
import static act.controller.Controller.Util.text;

public class Info {

    @GetAction("info")
    public static Object show(ActionContext context, App app) {
        if (context.acceptJson()) {
            String actVersion = Version.version();
            String appVersion = Version.appVersion(app.name());
            String appName = app.name();
            String pid = Env.PID.get();
            String baseDir = app.base().getAbsolutePath();
            String mode = Act.mode().name();
            String profile = Act.profile();
            String group = Act.nodeGroup();
            return jsonMap(actVersion, appVersion, appName, pid, baseDir, mode, profile, group);
        }
        return text(Banner.cachedBanner());
    }

    @GetAction("pid")
    public static Object pid(ActionContext context) {
        if (context.acceptJson()) {
            String pid = Env.PID.get();
            return jsonMap(pid);
        }
        return text(Env.PID.get());
    }

}
