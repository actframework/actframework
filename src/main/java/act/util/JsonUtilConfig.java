package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.app.App;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.mvc.MvcConfig;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.*;

import static com.alibaba.fastjson.JSON.DEFAULT_GENERATE_FEATURE;

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
        FastJsonSObjectCodec sObjectCodec = new FastJsonSObjectCodec();

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
        parserConfig.putDeserializer(ISObject.class, sObjectCodec);
        parserConfig.putDeserializer(SObject.class, sObjectCodec);

        MvcConfig.jsonSerializer(new $.F1<Object, String>() {
            @Override
            public String apply(Object o) throws NotAppliedException, Osgl.Break {
                if (null == o) {
                    return "{}";
                }
                ActContext<?> ctx = ActContext.Base.currentContext();
                SerializeFilter[] filters = null;
                SerializerFeature[] features = null;
                String dateFormatPattern = null;
                if (null != ctx) {
                    filters = ctx.fastjsonFilters();
                    features = ctx.fastjsonFeatures();
                    dateFormatPattern = ctx.dateFormatPattern();
                }
                Boolean b = DisableFastJsonCircularReferenceDetect.option.get();
                C.Set<SerializerFeature> featureSet = C.newSet();
                if (null != features) {
                    featureSet.addAll(C.listOf(features));
                }
                featureSet.add(SerializerFeature.WriteDateUseDateFormat);
                if (null != b && b) {
                    featureSet.add(SerializerFeature.DisableCircularReferenceDetect);
                }
                return null == dateFormatPattern ?
                        JSON.toJSONString(
                                o, filters,
                                featureSet.toArray(new SerializerFeature[featureSet.size()])) :
                        JSON.toJSONString(
                                o,
                                SerializeConfig.globalInstance,
                                filters,
                                dateFormatPattern,
                                DEFAULT_GENERATE_FEATURE,
                                featureSet.toArray(new SerializerFeature[featureSet.size()]));
            }
        });
    }
}
