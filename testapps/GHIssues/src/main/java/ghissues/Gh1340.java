package ghissues;

import act.data.annotation.Data;
import act.util.AdaptiveBeanBase;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.GetAction;

import java.util.ArrayList;
import java.util.List;

public class Gh1340 extends BaseController {
    
    public static class MyData extends AdaptiveBeanBase<MyData> {
        static MyData of(String key, Object val) {
            MyData myData = new MyData();
            myData.putValue(key, val);
            return myData;
        }
    }

    @GetAction("1340/adaptiveMap")
    public List<MyData> test() {
        List<MyData> list = new ArrayList<>();
        list.add(MyData.of("name", "foo").putValue("num", 2));
        list.add(MyData.of("name", "bar").putValue("num", 3));
        return list;
    }

    @GetAction("1340/map")
    public List<JSONObject> testMap() {
        List<JSONObject> list = new ArrayList<>();
        JSONObject json = new JSONObject();
        json.put("name", "foo");
        json.put("num", 3);
        list.add(json);
        json = new JSONObject();
        json.put("name", "bar");
        json.put("num", 2);
        list.add(json);
        return list;
    }

}
