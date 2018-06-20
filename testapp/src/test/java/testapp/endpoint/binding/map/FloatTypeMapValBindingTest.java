package testapp.endpoint.binding.map;

import org.junit.Ignore;
import org.osgl.util.C;

import java.util.Map;

public class FloatTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Float> {
    public FloatTypeMapValBindingTest() {
        super("float_v", "float_k");
    }

    @Override
    public Map<String, Float> nonEmptyMap() {
        return C.Map("a", Float.MAX_VALUE, "b", Float.MIN_VALUE, "c", 0.02f);
    }

    @Ignore
    @Override
    public void testKeyTypedNonEmptyMapGetFour() throws Exception {
        // float does not support param encoding type Four
    }

    @Ignore
    @Override
    public void testKeyTypedNonEmptyMapPostFour() throws Exception {
        // float does not support param encoding type Four
    }

}
