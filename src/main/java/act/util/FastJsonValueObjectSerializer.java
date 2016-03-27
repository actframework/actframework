package act.util;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.osgl.util.ValueObject;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Serialize {@link org.osgl.util.ValueObject} into JSON string
 */
public class FastJsonValueObjectSerializer implements ObjectSerializer {
    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.getWriter();
        if (object == null) {
            out.writeNull();
            return;
        }
        ValueObject vo = (ValueObject) object;
        out.write(vo.toJSONString());
    }
}
