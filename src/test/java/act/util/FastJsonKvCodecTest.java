package act.util;

import act.TestBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgl.util.KV;
import org.osgl.util.KVStore;

public class FastJsonKvCodecTest extends TestBase {

    private KV kv;

    @Before
    public void prepareData() {
        kv = new KVStore();
    }

    private void addInt() {
        kv.putValue("int", 3);
    }

    private void addIntAndStr() {
        addInt();
        kv.putValue("str", "foo");
    }

    private void addNestedKV() {
        addIntAndStr();
        KV kv0 = new KVStore();
        kv0.putValue("x", 6.6);
        kv.putValue("n1", kv0);
    }

    @BeforeClass
    public static void prepare() {
        SerializeConfig config = SerializeConfig.getGlobalInstance();
        config.put(KV.class, FastJsonKvCodec.INSTANCE);
        config.put(KVStore.class, FastJsonKvCodec.INSTANCE);

        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.putDeserializer(KV.class, FastJsonKvCodec.INSTANCE);
        parserConfig.putDeserializer(KVStore.class, FastJsonKvCodec.INSTANCE);

        JSON.DEFAULT_PARSER_FEATURE = Feature.config(JSON.DEFAULT_PARSER_FEATURE, Feature.UseBigDecimal, false);
    }

    @Test
    public void testSingleIntValue() {
        addInt();
        verify();
    }

    @Test
    public void testIntAndString() {
        addIntAndStr();
        verify();
    }

    @Test
    public void testNestedStructure() {
        addNestedKV();
        verify();
    }

    private void verify() {
        String s = JSON.toJSONString(kv);
        KV kv2 = JSON.parseObject(s, KV.class);
        eq(kv2, kv);
    }

}
