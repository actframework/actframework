package testapp.endpoint.binding.collection;

import org.junit.Ignore;
import org.osgl.util.C;

import java.util.List;

@Ignore
public class CharArrayActionParameterBindingTest extends PrimitiveTypeArrayActionParameterBindingTestBase<Character> {

    @Override
    protected String listPath() {
        return "char_list";
    }

    @Override
    protected String setPath() {
        return "char_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "char_wa";
    }

    @Override
    protected String primitiveArrayPath() {
        return "char_pa";
    }

    @Override
    protected List<Character> nonEmptyList() {
        return C.list(' ', 'a', 'x', 'a');
    }

}
