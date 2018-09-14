package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("832")
public class Gh832 {

    @GetAction("/post/{cityId}")
    public int action(int cityId) {
        return cityId;
    }

}
