package testapp.endpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.junit.Test;
import org.osgl.util.C;

import java.util.Map;

public class GHIssue301 extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        drop();
        create("foo", 100);
        create("foo", 120);
        reset();
        url("/gh/301").param("list", "foo").getJSON();
        String body = resp().body().string();
        JSONArray list = JSON.parseArray(body);
        eq(2, list.size());
    }

    private void drop() throws Exception {
        url("/gh/301").delete();
        checkRespCode();
    }

    private void create(String name, int age) throws Exception {
        reset();
        Map<String, Object> payload = C.Map("name", name, "age", age);
        url("/gh/301").postJSON(payload);
        checkRespCode();
    }

}
