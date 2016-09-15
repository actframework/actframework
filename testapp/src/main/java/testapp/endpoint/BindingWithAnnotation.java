package testapp.endpoint;

import act.controller.Controller;
import act.data.annotation.Pattern;
import org.joda.time.DateTime;
import org.osgl.mvc.annotation.GetAction;

@Controller("/bwa")
public class BindingWithAnnotation extends Controller.Util {

    @GetAction("datetime")
    public DateTime dateTime(@Pattern("yyyyMMdd") DateTime dateTime) {
        return dateTime;
    }

}
