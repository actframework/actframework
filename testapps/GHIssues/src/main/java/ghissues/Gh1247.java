package ghissues;

import act.controller.annotation.UrlContext;
import act.data.annotation.Data;
import act.inject.DefaultValue;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.osgl.mvc.annotation.GetAction;

import java.util.Date;

@UrlContext("1247")
public class Gh1247 extends BaseController {

    @GetAction("joda-localdate")
    public LocalDate testLocalDate(@DefaultValue("now") LocalDate date) {
        return date;
    }

    @GetAction("joda-datetime")
    public DateTime testDateTime(@DefaultValue("now") DateTime dateTime) {
        return dateTime;
    }

    @GetAction("jdk-date")
    public Date testDate(@DefaultValue("now") Date date) {
        return date;
    }

}
