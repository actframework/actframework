package act.data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.ValueObject;

import java.util.List;

public class ListCodec implements ValueObject.Codec<List<Object>> {

    @Override
    public Class<List<Object>> targetClass() {
        return $.cast(List.class);
    }

    @Override
    public List<Object> parse(String s) {
        JSONArray array = JSON.parseArray(s);
        List<Object> list = C.newSizedList(array.size());
        for (Object o : array) {
            list.add(ValueObject.of(o));
        }
        return list;
    }

    @Override
    public String toString(List<Object> o) {
        return JSON.toJSONString(o);
    }

    @Override
    public String toJSONString(List<Object> o) {
        return toString(o);
    }
}
