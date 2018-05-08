package testapp.endpoint.ghissues;

import act.util.JsonView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

import java.util.Date;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class GH657 extends GithubIssueBase {

    @GetAction("657/1")
    public List list1() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("date", new Date());
        return C.list(jsonObject);
    }

    @GetAction("657/2")
    @JsonView
    public void list2() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("date", new Date());
        renderJson(C.list(jsonObject));
    }

}
