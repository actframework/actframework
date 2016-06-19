package testapp.endpoint;

import org.osgl.util.C;

import java.util.List;

public class BooleanArrayParameterResolverTest extends SimpleTypeArrayParameterResolverTestBase<Boolean> {

    @Override
    protected String listPath() {
        return "bool_list";
    }

    @Override
    protected String setPath() {
        return "bool_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "bool";
    }

    @Override
    protected String primitiveArrayPath() {
        return "bool_p";
    }

    @Override
    protected List<Boolean> nonEmptyList() {
        return C.list(true, false, true, true);
    }

    @Override
    protected String expectedRespForNonEmptyList() {
        return "[true, false, true, true]";
    }

    @Override
    protected String expectedRespForNonEmptySet() {
        return "[false, true]";
    }
}
