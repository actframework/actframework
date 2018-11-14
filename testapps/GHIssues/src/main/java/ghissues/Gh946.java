package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.annotation.GetAction;

import java.util.Map;

@UrlContext("946")
public class Gh946 extends BaseController {

    @Configuration("map.field")
    private Map<String,String> map;

    @GetAction
    public Map test() {
        return map;
    }

}
