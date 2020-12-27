package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

import java.util.Date;
import java.util.List;

import static act.controller.Controller.Util.renderJson;

@UrlContext("1361")
public class Gh1361 extends BaseController {

    @LoadResource("1361.json")
    private JSONObject json;

    @GetAction
    public JSONObject get() {
        return json;
    }

}
