package testapp.endpoint.ghissues;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.osgl.mvc.annotation.GetAction;

public class GH691 extends GithubIssueBase {

    @GetAction("691/1")
    public DateTime test1(DateTime timestamp) {
        return timestamp;
    }

    @GetAction("691/2")
    public LocalDate test2(LocalDate timestamp) {
        return timestamp;
    }
}
