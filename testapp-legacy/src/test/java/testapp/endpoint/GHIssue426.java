package testapp.endpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import org.osgl.http.H;

public class GHIssue426 extends EndpointTester {

    @Test
    public void testFastJsonFilter() throws Exception {
        url("/gh/426").get("name", "Tom", "count", 3).accept(H.Format.JSON);
        JSONObject obj = JSON.parseObject(resp().body().string());
        eq("Tom", obj.getString("Name"));
        eq(3, obj.getInteger("BarCount"));
        // TODO uncomment the following line once
        // we get https://github.com/alibaba/fastjson/issues/1635 fixed
        // in FastJSON
        //no(obj.getBoolean("Flag"));
    }

}
