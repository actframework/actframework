package ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Date;
import java.util.List;

import static act.controller.Controller.Util.renderJson;

@UrlContext("1379")
public class Gh1379 extends BaseController {

    @GetAction("{foo}/{bar}")
    public String test(String foo, String bar) {
        return S.fmt("%s-%s", foo, bar);
    }

    @GetAction("2/{foo}-{bar}")
    public String test2(String foo, String bar) {
        return test(foo, bar);
    }

    @GetAction("3/{foo}/at/{bar}")
    public String test3(String foo, String bar) {
        return test(foo, bar);
    }

}
