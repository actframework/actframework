package testapp.endpoint.issue.enhancement;

import org.junit.Test;
import testapp.endpoint.EndpointTester;

import java.io.IOException;

/**
 * Test A Controller action method enhancement failure
 * found on 11-Sep-2016
 */
public class Controler20160911 extends EndpointTester {

    @Test
    public void test() throws IOException {
        url("/issue/enhancer/20160911").param("status", 511);
        eq(511, resp().code());
    }

}
