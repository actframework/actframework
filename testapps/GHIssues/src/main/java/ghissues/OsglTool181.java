package ghissues;

import act.controller.annotation.UrlContext;
import act.util.AdaptiveBean;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;

// for https://github.com/osglworks/java-tool/issues/181
@UrlContext("osgl-tool/181")
public class OsglTool181 extends BaseController {
    public static class Kit extends AdaptiveBean {
    }

    @GetAction
    public Kit test181() {
        Kit kit = new Kit();
        kit.putValue("DisplayName", "Primary Kit - kids aged 5-9 (unavailable - restock due 2019)");

        JSONObject newData = new JSONObject();
        newData.put("DisplayName", "Primary Kit - kids aged 5-9");
        $.merge(newData).to(kit);

        return kit;
    }

}
