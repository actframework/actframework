package testapp.endpoint.binding;

import org.junit.Test;
import testapp.endpoint.EndpointTester;
import testapp.endpoint.ParamAndField;

/**
 * Test parameter binding to Field
 */
public class ParamAndFieldTest extends EndpointTester {

    private static final String PATH = ParamAndField.PATH;

    @Test
    public void test() throws Exception {
        url(PATH).get("foo", "foo", "bar", "bar");
        bodyEq("foobar");
    }

}
