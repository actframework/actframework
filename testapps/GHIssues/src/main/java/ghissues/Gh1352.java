package ghissues;

import static act.controller.Controller.Util.renderJson;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.C;

import java.io.File;
import java.util.Date;
import java.util.List;

@UrlContext("1352")
public class Gh1352 extends BaseController {

    @PostAction
    public boolean test(String[] selectValues, File file, File[] files) {
        return null != file;
    }

}
