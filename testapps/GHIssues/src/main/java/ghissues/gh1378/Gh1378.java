package ghissues.gh1378;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import act.db.ebean.EbeanDao;
import ghissues.BaseController;
import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PutAction;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import static act.controller.Controller.Util.notFoundIfNull;

@UrlContext("1378")
public class Gh1378 extends BaseController {

    @Inject
    private EbeanDao<Integer, Category> dao;

    @PutAction("{id}")
    public void update(@DbBind("id") @NotNull Category cate, Category category) {
        notFoundIfNull(cate);
        //$.merge(category).filter("-id").to(cate);
        cate.name = category.name;
        dao.save(cate);
    }

    @GetAction("{id}")
    public Category findById(int id) {
        return dao.findById(id);
    }
}
