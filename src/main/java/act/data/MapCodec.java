package act.data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.ValueObject;

import java.util.Map;

public class MapCodec implements ValueObject.Codec<Map<String, Object>> {
    @Override
    public Class<Map<String, Object>> targetClass() {
        return $.cast(Map.class);
    }

    @Override
    public Map<String, Object> parse(String s) {
        JSONObject json = JSON.parseObject(s);
        Map<String, Object> map = C.newMap();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            map.put(entry.getKey(), ValueObject.of(entry.getValue()));
        }
        return map;
    }

    @Override
    public String toString(Map<String, Object> o) {
        return JSON.toJSONString(o);
    }

    @Override
    public String toJSONString(Map<String, Object> o) {
        return toString(o);
    }
}
