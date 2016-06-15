package testapp.endpoint;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;
import testapp.EndpointTester;

public class HelloControllerTest extends EndpointTester {

    @Test
    public void testHello1() throws Exception {
        Request request = new Request.Builder().url("http://localhost:6111/hello1").build();
        Response response = http.newCall(request).execute();
        eq("hello", response.body().string());
    }

}
