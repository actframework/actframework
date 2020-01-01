package ghissues;

import act.apidoc.SampleData;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;

public class Gh1261 extends BaseController {

    @GetAction("1261")
    public List<String> test() {
        return SampleData.generateList(String.class);
    }

}
