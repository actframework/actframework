package testapp.endpoint.binding.single;

import org.junit.Test;
import org.osgl.mvc.result.NotFound;
import testapp.endpoint.EndPointTestContext.RequestMethod;
import testapp.endpoint.binding.ActionParameterBindingTestBase;

public abstract class SimpleTypeActionParameterBindingTestBase<T> extends ActionParameterBindingTestBase {

    protected static final String PARAM = "v";

    protected String path;

    public SimpleTypeActionParameterBindingTestBase() {
        this.path = urlPath();
    }


    protected abstract String urlPath();

    protected abstract T nonNullValue();

    protected String expectedRespForNonNullValue() {
        return nonNullValue().toString();
    }

    protected final String e() {
        return expectedRespForNonNullValue();
    }

    @Override
    protected String urlContext() {
        return "/spr";
    }

    protected final void _verify(String expected, String urlPath, Object data, RequestMethod method) throws Exception {
        if (null != data) {
            context.params(PARAM, data);
        }
        context
                .expected(expected)
                .url(processUrl(urlPath))
                .method(method)
                .applyTo(this);
    }

    @Test(expected = NotFound.class)
    public void testNullGet() throws Exception {
        _verify("", path, null, RequestMethod.GET);
    }

    @Test(expected = NotFound.class)
    public void testNullPostForm() throws Exception {
        _verify("", path, null, RequestMethod.POST_FORM_DATA);
    }

    @Test(expected = NotFound.class)
    public void testNullPostJSON() throws Exception {
        _verify("", path, null, RequestMethod.POST_JSON);
    }

    @Test
    public void testNotNullGet() throws Exception {
        _verify(e(), path, nonNullValue(), RequestMethod.GET);
    }

    @Test
    public void testNotNullPostForm() throws Exception {
        _verify(e(), path, nonNullValue(), RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNotNullPostJSON() throws Exception {
        _verify(e(), path, nonNullValue(), RequestMethod.POST_JSON);
    }
}
