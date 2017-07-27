package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import org.osgl.mvc.annotation.GetAction;
import testapp.model.mongo.GH301Model;

import javax.validation.constraints.NotNull;

/**
 * Test Github #319
 */
@UrlContext("319")
public class GH319 extends GithubIssueBase {

    @GetAction("{id}")
    public GH301Model test(@DbBind("id") @NotNull GH301Model model) {
        return model;
    }

}
