package testapp.endpoint;

import org.junit.Test;

public class GHIssue434 extends EndpointTester {

    @Test
    public void testMap() throws Exception {
        url("/gh/434/by_lang").get();
        checkRespCode();
    }

    @Test
    public void testInject() throws Exception {
        url("/gh/434/inject").get();
        checkRespCode();
    }

    @Test
    public void testConfigured() throws Exception {
        url("/gh/434/conf").get();
        checkRespCode();
    }

    @Test
    public void testSetting() throws Exception {
        url("/gh/434/setting").get();
        checkRespCode();
    }

}
