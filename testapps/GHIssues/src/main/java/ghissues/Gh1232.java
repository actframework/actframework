package ghissues;

import act.controller.ExpressController;
import act.controller.annotation.UrlContext;
import act.handler.NonBlock;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static act.controller.Controller.Util.renderJson;

@UrlContext("1232")
@ExpressController
public class Gh1232 extends BaseController {

    public static class Foo {
        public String name;

        public Foo(String name) {
            this.name = name;
        }
    }

    @GetAction
    @NonBlock
    public Foo test() {
        return new Foo(S.repeat("bar").x(1000 * 100));
    }

}
