package testapp.endpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.junit.Test;
import org.osgl.util.S;

public class GHIssue310 extends EndpointTester {

    @Test
    public void test() throws Exception {
        drop();
        create(S.random());
        create(S.random());
        create("foo");
        url("/gh/310").getJSON();
        JSONArray array = JSON.parseArray(resp().body().string());
        eq(3, array.size());
        setup();
        url("/gh/310/name/foo").getJSON();
        array = JSON.parseArray(resp().body().string());
        eq(1, array.size());
    }

    private void drop() throws Exception {
        url("/gh/310").delete();
        checkRespCode();
        setup();
    }

    private void create(String name) throws Exception {
        url("/gh/310?model.name=" + name).post();
        checkRespCode();
        setup();
    }

}
