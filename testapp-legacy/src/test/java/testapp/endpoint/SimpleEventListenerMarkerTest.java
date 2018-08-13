package testapp.endpoint;

import org.junit.Test;

import java.io.IOException;

public class SimpleEventListenerMarkerTest extends EndpointTester  {

    @Test
    public void test() throws IOException {
        url("/event/custom_marker").get();
        checkRespCode();
    }

}
