package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.NotModified;

import java.io.IOException;

public class ETagTest extends EndpointTester {

    @Test(expected = NotModified.class)
    public void itShallReturnNotModifiedIfETagMatches() throws IOException {
        String etag = retrieveETag();
        setup();
        url("/etag/%s", etag).header("If-None-Match", etag).get();
        bodyEq("");
    }

    public String retrieveETag() throws IOException {
        url("/etag");
        return resp().header("ETag");
    }


}
