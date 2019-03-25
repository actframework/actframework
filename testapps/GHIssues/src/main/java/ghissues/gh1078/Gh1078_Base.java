package ghissues.gh1078;

import ghissues.BaseController;
import org.osgl.mvc.annotation.After;
import org.osgl.mvc.result.RenderJSON;
import org.osgl.util.C;

public abstract class Gh1078_Base extends BaseController {

    @After
    public void after() {
        throw new RenderJSON(C.Map("result", "intercepted"));
    }

}
