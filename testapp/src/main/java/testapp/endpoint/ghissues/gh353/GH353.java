package testapp.endpoint.ghissues.gh353;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.annotation.PutAction;
import testapp.endpoint.ghissues.GithubIssueBase;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Map;

@UrlContext("353")
public class GH353 extends GithubIssueBase {

    @Inject
    User.Dao dao;

    @DeleteAction
    public void drop() {
        dao.drop();
    }

    @PostAction
    public User create(User user) {
        return dao.save(user);
    }

    @PutAction("{user}")
    public User update(@DbBind @NotNull User user, Map<String, Object> props) {
        user.mergeValues(props);
        return dao.save(user);
    }

}
