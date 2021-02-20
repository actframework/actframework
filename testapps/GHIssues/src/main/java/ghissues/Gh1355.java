package ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.C;

import java.util.Date;
import java.util.List;

import static act.controller.Controller.Util.renderJson;

@UrlContext("1355")
public class Gh1355 extends BaseController {

    @PostAction
    public JSONObject test(Long com_id, JSONObject data) {
        data.put("_id", com_id);
        return data;
    }
}
