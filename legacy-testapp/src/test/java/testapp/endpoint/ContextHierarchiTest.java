package testapp.endpoint;

import org.junit.Test;

/**
 * Refer to https://github.com/actframework/actframework/issues/88
 */
public class ContextHierarchiTest extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/cht/cht_test");
        checkRespCode();
    }

}
