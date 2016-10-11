package testapp.endpoint;

import act.cli.Command;
import act.controller.Controller;
import act.data.annotation.ReadContent;
import org.osgl.mvc.annotation.PostAction;

import java.util.List;

@Controller("/misc")
@SuppressWarnings("unused")
public class MiscsTestBed extends Controller.Util {

    @PostAction("read")
    @Command("misc.cat")
    public String testReadContentIgnoreError(
            @ReadContent String content
    ) {
        return content;
    }

    @PostAction("readLine")
    @Command("misc.catLine")
    public List<String> testReadLinesIgnoreError(
            @ReadContent List<String> content
    ) {
        return content;
    }

}
