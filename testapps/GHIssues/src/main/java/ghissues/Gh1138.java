package ghissues;

import act.controller.annotation.UrlContext;
import act.job.Every;
import act.job.OnAppStart;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

import javax.inject.Singleton;
import java.util.Date;
import java.util.List;

import static act.controller.Controller.Util.renderJson;

@UrlContext("1138")
@Singleton
public class Gh1138 extends BaseController {

    private int n;

    @OnAppStart
    @Every(value = "1s", startImmediately = false)
    public void incr() {
        n++;
    }

    @GetAction
    public int get() {
        return n;
    }

}
