package testapp.endpoint;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.osgl.mvc.result.NotFound;
import org.osgl.util.C;

import java.util.Map;

public class GHIssue297 extends EndpointTester {

    @Test(expected = NotFound.class)
    public void testFoo() throws Exception {
        Map<String, Object> payload = C.map("person", new ObjectId(), "list", new String[] {"1", "a"});
        url("/gh/297/").postJSON(payload);
        checkRespCode();
    }

}
