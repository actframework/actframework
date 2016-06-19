package testapp.endpoint;

import org.junit.Test;
import testapp.EndpointTester;

public class ParamVsFieldTest extends EndpointTester {

    private static final String PATH = ParamVsField.PATH;

    @Test
    public void test() throws Exception {
        url(PATH).get("foo", "foo", "bar", "bar");
        bodyEq("foobar");
    }

}
