package act.util;

import act.app.App;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.mvc.MvcConfig;
import org.osgl.util.*;

public class JsonUtilConfig {
    public static void configure(App app) {
        SerializeConfig config = SerializeConfig.getGlobalInstance();

        // patch https://github.com/alibaba/fastjson/issues/478
        config.put(FastJsonIterable.class, FastJsonIterableSerializer.instance);

        FastJsonJodaDateCodec jodaDateCodec = new FastJsonJodaDateCodec(app);
        app.registerSingleton(FastJsonJodaDateCodec.class, jodaDateCodec);

        FastJsonValueObjectSerializer valueObjectSerializer = new FastJsonValueObjectSerializer();
        app.registerSingleton(FastJsonValueObjectSerializer.class, valueObjectSerializer);
        FastJsonKeywordCodec keywordCodec = new FastJsonKeywordCodec();

        config.put(DateTime.class, jodaDateCodec);
        config.put(LocalDate.class, jodaDateCodec);
        config.put(LocalTime.class, jodaDateCodec);
        config.put(LocalDateTime.class, jodaDateCodec);
        config.put(ValueObject.class, valueObjectSerializer);
        config.put(Keyword.class, keywordCodec);
        config.put(KV.class, FastJsonKvCodec.INSTANCE);
        config.put(KVStore.class, FastJsonKvCodec.INSTANCE);

        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.putDeserializer(DateTime.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalDate.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalTime.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalDateTime.class, jodaDateCodec);
        parserConfig.putDeserializer(Keyword.class, keywordCodec);
        parserConfig.putDeserializer(KV.class, FastJsonKvCodec.INSTANCE);
        parserConfig.putDeserializer(KVStore.class, FastJsonKvCodec.INSTANCE);

        MvcConfig.jsonSerializer(new $.F1<Object, String>() {
            @Override
            public String apply(Object o) throws NotAppliedException, Osgl.Break {
                Boolean b = DisableFastJsonCircularReferenceDetect.option.get();
                if (null != b && b) {
                    return JSON.toJSONString(o, SerializerFeature.WriteDateUseDateFormat, SerializerFeature.DisableCircularReferenceDetect);
                } else {
                    return JSON.toJSONString(o, SerializerFeature.WriteDateUseDateFormat);
                }
            }
        });
    }
}
