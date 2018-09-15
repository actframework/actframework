package ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import com.alibaba.fastjson.annotation.JSONField;
import org.osgl.mvc.annotation.GetAction;

import java.util.Date;

@UrlContext("798")
@JsonView
public class Gh798 extends BaseController {

    public static class Foo {
        @JSONField(format = "yyyy-MM")
        public Date date = new Date();
    }

    @GetAction
    public Foo foo() {
        return new Foo();
    }

}
