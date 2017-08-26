package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import com.alibaba.fastjson.JSONException;
import org.osgl.mvc.annotation.Catch;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

import javax.xml.stream.XMLStreamException;

@UrlContext("350")
public class GH350 extends GithubIssueBase {

    @Catch
    public Result handleException(XMLStreamException r1, JSONException r2) {
        if (null != r1) {
            return renderText("xml");
        }
        return renderText("json");
    }

    @GetAction("_xml")
    public void test350a() throws Exception {
        throw new XMLStreamException("");
    }

    @GetAction("_json")
    public void test350b() throws Exception {
        throw new JSONException("");
    }

    @GetAction("_ex")
    public void test350c() {
        throw new IllegalArgumentException();
    }

}
