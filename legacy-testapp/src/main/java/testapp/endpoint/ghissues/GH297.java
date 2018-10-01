package testapp.endpoint.ghissues;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.db.DbBind;
import org.osgl.mvc.annotation.PostAction;
import testapp.model.mongo.Person;

import java.util.Set;

/**
 * Test Github #297 issue
 */
@UrlContext("297")
public class GH297 extends GithubIssueBase {

    @PostAction
    public Person test(@DbBind Person person, Set<String> list, ActionContext context) {
        return person;
    }

}
