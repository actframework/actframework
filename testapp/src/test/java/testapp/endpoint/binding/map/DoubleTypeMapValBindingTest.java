package testapp.endpoint.binding.map;

import org.junit.Ignore;
import org.osgl.util.C;

import java.util.Map;

public class DoubleTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Double> {
    public DoubleTypeMapValBindingTest() {
        super("double_v", "double_k");
    }

    @Override
    public Map<String, Double> nonEmptyMap() {
        return C.Map("a", Double.MAX_VALUE, "b", Double.MIN_VALUE, "c", 0.02d);
    }

    @Ignore
    @Override
    public void testKeyTypedNonEmptyMapGetFour() throws Exception {
        // double does not support param encoding type Four
    }

    @Ignore
    @Override
    public void testKeyTypedNonEmptyMapPostFour() throws Exception {
        // double does not support param encoding type Four
    }
}
