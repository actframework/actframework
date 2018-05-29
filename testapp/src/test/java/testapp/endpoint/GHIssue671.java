package testapp.endpoint;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

public class GHIssue671 extends EndpointTester {

    @Test
    public void testList1() throws Exception {
        url("/gh/671").get();
        JSONObject json = bodyJSONObject();
        eq("***", json.get("key"));
        eq("http://apis.juhe.cn/mobile/get", json.get("uri"));
    }

}
