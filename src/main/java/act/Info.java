package act;

import act.app.ActionContext;
import act.app.App;
import act.sys.Env;
import act.util.Banner;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

import static act.controller.Controller.Util.jsonMap;
import static act.controller.Controller.Util.text;

public class Info {

    @GetAction("/~info")
    public static Object show(ActionContext context, App app) {
        if (context.acceptJson()) {
            String actVersion = Version.version();
            String appVersion = Version.appVersion();
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

    @GetAction("/~pid")
    public static Object pid(ActionContext context) {
        if (context.acceptJson()) {
            String pid = Env.PID.get();
            return jsonMap(pid);
        }
        return text(Env.PID.get());
    }

}
