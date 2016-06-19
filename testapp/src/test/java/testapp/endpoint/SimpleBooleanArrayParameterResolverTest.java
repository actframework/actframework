package testapp.endpoint;

import org.junit.Test;

public class SimpleBooleanArrayParameterResolverTest extends ParameterResolverTestBase {

    private static final String PARAM = "v";

    @Override
    protected String urlContext() {
        return "/sapr";
    }

    @Test
    public void getPrimitiveBooleanEmptyArray() throws Exception {
        final boolean[] ba = new boolean[0];
        final String url = processUrl("bool_p");
        verifyGet("[]", url, PARAM, ba);
    }

}
