package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import org.osgl.mvc.annotation.GetAction;

import java.util.Map;

import static act.controller.Controller.Util.notFoundIfNull;

@UrlContext("1146")
public class Gh1146 extends BaseController {

    @LoadResource("1146.map")
    private Map<String, String> map;

    @GetAction
    public Map<String, String> makeDouble() {
        return map;
    }

}
