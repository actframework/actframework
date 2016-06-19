package testapp.endpoint;

import org.osgl.util.C;

import java.util.List;

public class ByteArrayParameterResolverTest extends SimpleTypeArrayParameterResolverTestBase<Byte> {

    @Override
    protected String listPath() {
        return "byte_list";
    }

    @Override
    protected String setPath() {
        return "byte_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "byte";
    }

    @Override
    protected String primitiveArrayPath() {
        return "byte_p";
    }

    @Override
    protected List<Byte> nonEmptyList() {
        return C.list(b(-1), b(0), b(-1), b(126));
    }

    @Override
    protected String expectedRespForNonEmptyList() {
        return "[-1, 0, -1, 126]";
    }

    @Override
    protected String expectedRespForNonEmptySet() {
        return "[0, 126, -1]";
    }

    private byte b(int i) {
        return (byte) i;
    }
}
