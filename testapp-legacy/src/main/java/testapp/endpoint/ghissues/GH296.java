package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.PostAction;
import testapp.model.mongo.Person;

/**
 * Test Github #296 issue
 */
@UrlContext("296")
public class GH296 extends GithubIssueBase {

    @PostAction
    public String test(Person[] person) {
        return person[0].getName();
    }

}
