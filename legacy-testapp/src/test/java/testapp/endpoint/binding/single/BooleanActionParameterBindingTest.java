package testapp.endpoint.binding.single;

import org.junit.Test;
import testapp.endpoint.EndPointTestContext;

public class BooleanActionParameterBindingTest extends PrimitiveTypeActionParameterBindingTestBase<Boolean> {
    @Override
    protected String primitiveUrlPath() {
        return "bool_p";
    }

    @Override
    protected String urlPath() {
        return "bool_w";
    }

    @Override
    protected Boolean nonNullValue() {
        return true;
    }

    @Override
    protected String expectedRespForNonNullValue() {
        return "true";
    }

    @Override
    protected String primitiveDefValueStr() {
        return "false";
    }

    @Override
    protected Object outOfScopeValue() {
        return 111;
    }

    private String def() {
        return primitiveDefValueStr();
    }

    private Object out() {
        return outOfScopeValue();
    }


    @Test
    public void testPrimitiveOutOfScopeGet() throws Exception {
        _verify(def(), primitiveUrlPath, out(), EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveOutOfScopePostForm() throws Exception {
        _verify(def(), primitiveUrlPath, out(), EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveOutOfScopePostJSON() throws Exception {
        _verify(def(), primitiveUrlPath, out(), EndPointTestContext.RequestMethod.POST_JSON);
    }

    @Test
    public void testWrapOutOfScopeGet() throws Exception {
        _verify(def(), path, out(), EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testWrapOutOfScopePostForm() throws Exception {
        _verify(def(), path, out(), EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapOutOfScopePostJSON() throws Exception {
        _verify(def(), path, out(), EndPointTestContext.RequestMethod.POST_JSON);
    }
}
