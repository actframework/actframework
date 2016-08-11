package testapp.endpoint.binding.single;

import org.junit.Test;
import org.osgl.mvc.result.BadRequest;
import testapp.endpoint.EndPointTestContext;

public abstract class PrimitiveTypeActionParameterBindingTestBase<T> extends SimpleTypeActionParameterBindingTestBase<T> {
    protected String primitiveUrlPath;

    public PrimitiveTypeActionParameterBindingTestBase() {
        this.primitiveUrlPath = primitiveUrlPath();
    }

    protected abstract String primitiveUrlPath();

    protected abstract String primitiveDefValueStr();

    protected abstract Object outOfScopeValue();

    private String def() {
        return primitiveDefValueStr();
    }

    private Object out() {
        return outOfScopeValue();
    }

    @Test
    public void testPrimitiveNullGet() throws Exception {
        _verify(def(), primitiveUrlPath, null, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullPostForm() throws Exception {
        _verify(def(), primitiveUrlPath, null, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullPostJSON() throws Exception {
        _verify(def(), primitiveUrlPath, null, EndPointTestContext.RequestMethod.POST_JSON);
    }

    @Test
    public void testPrimitiveNotNullGet() throws Exception {
        _verify(e(), primitiveUrlPath, nonNullValue(), EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNotNullPostForm() throws Exception {
        _verify(e(), primitiveUrlPath, nonNullValue(), EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNotNullPostJSON() throws Exception {
        _verify(e(), primitiveUrlPath, nonNullValue(), EndPointTestContext.RequestMethod.POST_JSON);
    }


    @Test(expected = BadRequest.class)
    public void testPrimitiveOutOfScopeGet() throws Exception {
        _verify("", primitiveUrlPath, out(), EndPointTestContext.RequestMethod.GET);
    }

    @Test(expected = BadRequest.class)
    public void testPrimitiveOutOfScopePostForm() throws Exception {
        _verify("", primitiveUrlPath, out(), EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test(expected = BadRequest.class)
    public void testPrimitiveOutOfScopePostJSON() throws Exception {
        _verify("", primitiveUrlPath, out(), EndPointTestContext.RequestMethod.POST_JSON);
    }

    @Test(expected = BadRequest.class)
    public void testWrapOutOfScopeGet() throws Exception {
        _verify("", path, out(), EndPointTestContext.RequestMethod.GET);
    }

    @Test(expected = BadRequest.class)
    public void testWrapOutOfScopePostForm() throws Exception {
        _verify("", path, out(), EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test(expected = BadRequest.class)
    public void testWrapOutOfScopePostJSON() throws Exception {
        _verify("", path, out(), EndPointTestContext.RequestMethod.POST_JSON);
    }

    public static void main(String[] args) {
        System.out.println(Integer.parseInt("127", 8));
    }
}
