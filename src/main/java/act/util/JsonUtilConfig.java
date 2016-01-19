package act.util;

import act.app.App;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public class JsonUtilConfig {
    public static void configure(App app) {
        SerializeConfig config = SerializeConfig.getGlobalInstance();

        // patch https://github.com/alibaba/fastjson/issues/478
        config.put(FastJsonIterable.class, FastJsonIterableSerializer.instance);

        FastJsonJodaDateCodec jodaDateCodec = new FastJsonJodaDateCodec(app);
        app.singletonRegistry().register(FastJsonJodaDateCodec.class, jodaDateCodec);

        config.put(DateTime.class, jodaDateCodec);
        config.put(LocalDate.class, jodaDateCodec);
        config.put(LocalTime.class, jodaDateCodec);
        config.put(LocalDateTime.class, jodaDateCodec);

        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.putDeserializer(DateTime.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalDate.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalTime.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalDateTime.class, jodaDateCodec);
    }
}
