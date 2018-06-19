package testapp.endpoint;

import act.db.morphia.util.FastJsonObjectIdCodec;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.bson.types.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgl.mvc.result.NotFound;
import org.osgl.util.C;

import java.util.Map;

public class GHIssue297 extends EndpointTester {

    @BeforeClass
    public static void prepareFastJson() {
        FastJsonObjectIdCodec objectIdCodec = new FastJsonObjectIdCodec();

        SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
        serializeConfig.put(ObjectId.class, objectIdCodec);
    }

    @Test(expected = NotFound.class)
    public void testFoo() throws Exception {
        Map<String, Object> payload = C.Map("person", new ObjectId(), "list", new String[] {"1", "a"});
        url("/gh/297/").postJSON(payload);
        checkRespCode();
    }

}
