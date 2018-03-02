package testapp.endpoint;

import org.junit.Test;

public class GHIssue538 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/538").header("Content-Type", "application/www-form-urlencoded; charset=UTF-8")
                .post().params("list[0][id]",1, "list[0][name]","foo","list[1][id]",2, "list[1][name]","bar");
        bodyEq("[{\"id\":1,\"name\":\"foo\"},{\"id\":2,\"name\":\"bar\"}]");
    }

}
