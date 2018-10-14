package testapp.endpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.junit.Test;
import org.osgl.$;
import org.osgl.util.C;

import java.util.List;

public class GHIssue310 extends EndpointTester {

    @Test
    public void test() throws Exception {
        drop();
        System.out.println(1);
        create(random());
        System.out.println(2);
        create(random());
        System.out.println(3);
        create("foo");
        System.out.println(4);
        url("/gh/310").getJSON();
        System.out.println(5);
        JSONArray array = JSON.parseArray(resp().body().string());
        System.out.println(6);
        eq(3, array.size());
        setup();
        url("/gh/310/name/foo").getJSON();
        System.out.println(7);
        array = JSON.parseArray(resp().body().string());
        System.out.println(8);
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

    private static List<String> nameList = C.list("tom1", "betty2", "zama3");

    private String random() {
        return $.random(nameList);
    }

}
