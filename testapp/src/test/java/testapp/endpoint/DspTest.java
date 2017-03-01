package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.Conflict;

import java.io.IOException;

public class DspTest extends EndpointTester {

    @Test(expected = Conflict.class)
    public void postShallReturnUnauthorizedWhenCsrfTokenNotProvided() throws IOException {
        url("/dsp").post("act_dsp_token", "12345");
        bodyEq("201 Created");
        String session = resp().header("X-TEST-Session");
        setup();
        url("/dsp").header("X-TEST-Session", session).post("act_dsp_token", "12345");
        bodyEq("201 Created");
    }
}
