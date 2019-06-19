package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static act.controller.Controller.Util.notFoundIfNull;
import static act.controller.Controller.Util.renderJson;

@UrlContext("1147")
public class Gh1147 extends BaseController {

    @LoadResource("1147.map")
    private Map<String, Integer> map;

    @GetAction
    public Integer makeDouble(String key) {
        Integer I = map.get(key);
        notFoundIfNull(I);
        return I * 2;
    }

}
