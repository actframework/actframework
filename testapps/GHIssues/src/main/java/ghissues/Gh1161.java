package ghissues;

import act.controller.annotation.UrlContext;
import act.db.jpa.JPADao;
import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.C;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;
import java.util.List;

import static act.controller.Controller.Util.renderJson;

@UrlContext("1161")
public class Gh1161 extends BaseController {
    @Entity(name = "m1161")
    public static class ModelB {
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Id
        public Integer id;
        public String name;
    }

    public static class Dao extends JPADao<Integer, ModelB> {

    }

    @Inject
    private Dao dao;

    @GetAction("{name}")
    public ModelB get(String name) {
        return dao.findOneBy("name", name);
    }

    @PostAction("nil")
    public void doNothing() {}
}
