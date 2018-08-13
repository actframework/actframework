package testapp.endpoint;

import org.junit.Test;
import org.osgl.$;
import org.osgl.mvc.result.NotModified;

import java.io.IOException;

public class ETagTest extends EndpointTester {

    @Test(expected = NotModified.class)
    public void itShallReturnNotModifiedIfETagMatches() throws IOException {
        $.Var<String> version = $.var();
        String etag = retrieveETag(version);
        setup();
        url("/etag/%s", version.get()).header("If-None-Match", etag).get();
        bodyEq("");
    }

    public String retrieveETag($.Var<String> version) throws IOException {
        url("/etag");
        version.set(resp().body().string());
        return resp().header("Etag");
    }


}
