package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import act.db.morphia.MorphiaDao;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import testapp.model.mongo.GH301Model;

import java.util.List;

/**
 * Test Github #301
 */
@UrlContext("301")
public class GH301 extends GithubIssueBase {

    @GetAction
    public List<GH301Model> test(@DbBind(field = "name") List<GH301Model> list) {
        return list;
    }

    @PostAction
    public GH301Model create(GH301Model model, MorphiaDao<GH301Model> dao) {
        return dao.save(model);
    }

    @DeleteAction
    public void drop(MorphiaDao<GH301Model> dao) {
        dao.drop();
    }

}
