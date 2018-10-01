package testapp.endpoint.ghissues.gh310;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import act.db.morphia.MorphiaDao;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import testapp.endpoint.ghissues.GithubIssueBase;

import javax.inject.Inject;

@UrlContext("310")
public class GH310 extends GithubIssueBase{

    @Inject
    private MorphiaDao<GH310Model> dao;

    @GetAction
    public Iterable<GH310Model> list(@DbBind Iterable<GH310Model> models) {
        return models;
    }

    @GetAction("name/{foos}")
    public Iterable<GH310Model> listFoo(@DbBind(field = "name") Iterable<GH310Model> foos) {
        return foos;
    }

    @PostAction
    public GH310Model create(GH310Model model) {
        return dao.save(model);
    }

    @DeleteAction
    public void drop() {
        dao.drop();
    }

}
