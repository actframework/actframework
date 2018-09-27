package ghissues;

import static act.controller.Controller.Util.renderJson;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

import java.util.Date;
import java.util.List;

@UrlContext("657")
public class Gh657 extends BaseController {

    @GetAction("list")
    @JsonView
    public List list() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("date", new Date());
        return C.list(jsonObject);
    }

    @GetAction("list1")
    public void list1() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("date", new Date());
        renderJson(C.list(jsonObject));
    }
}
