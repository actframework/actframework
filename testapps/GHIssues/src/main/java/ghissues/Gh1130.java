package ghissues;

import act.controller.annotation.UrlContext;
import act.util.FastJsonPropertyNamingStrategy;
import com.alibaba.fastjson.PropertyNamingStrategy;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1130")
public class Gh1130 extends BaseController {

    public static class Gh1130Model {
        public int fooBar = 1;
    }

    @GetAction
    @FastJsonPropertyNamingStrategy(PropertyNamingStrategy.SnakeCase)
    public Gh1130Model test() {
        return new Gh1130Model();
    }

}
