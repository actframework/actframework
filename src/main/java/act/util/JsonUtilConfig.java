package act.util;

import com.alibaba.fastjson.serializer.SerializeConfig;

public class JsonUtilConfig {
    public static void config() {
        SerializeConfig config = SerializeConfig.getGlobalInstance();

        // patch https://github.com/alibaba/fastjson/issues/478
        config.put(FastJsonIterable.class, FastJsonIterableSerializer.instance);
    }
}
