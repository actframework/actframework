package ghissues;

import act.controller.annotation.UrlContext;
import act.util.LogSupport;
import cn.hutool.http.HttpUtil;
import org.osgl.mvc.annotation.PostAction;

import java.util.HashMap;
import java.util.Map;

@UrlContext("830")
public class Gh830 extends LogSupport {

    @PostAction("svc/{url}")
    public String server(String url,String dataJson) {
        info("data: " + dataJson);
        info("url: "  + url);
        return url;
    }

    @PostAction("client/{url}")
    public String client(String url, String dataJson) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("dataJson", dataJson);
        String post = HttpUtil.post("http://localhost:5460/830/svc/" + url, map);
        info("response from server: " + post);
        return post;
    }


}
