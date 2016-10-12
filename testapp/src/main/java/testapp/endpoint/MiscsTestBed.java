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
    public String testReadContentMercy(
            @ReadContent(mercy = true) String content
    ) {
        return content;
    }

    @PostAction("readLine")
    @Command("misc.catLine")
    public List<String> testReadLinesMercy(
            @ReadContent(mercy = true) List<String> content
    ) {
        return content;
    }


    @PostAction("hardRead")
    @Command("misc.cat.hard")
    public String testReadContent(
            @ReadContent String content
    ) {
        return content;
    }

    @PostAction("hardReadLine")
    @Command("misc.catLine.hard")
    public List<String> testReadLines(
            @ReadContent List<String> content
    ) {
        return content;
    }

}
