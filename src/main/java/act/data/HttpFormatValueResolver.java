package act.data;

import org.osgl.http.H;

public class HttpFormatValueResolver extends StringValueResolverPlugin<H.Format> {
    @Override
    protected Class<H.Format> targetType() {
        return H.Format.class;
    }

    @Override
    public H.Format resolve(String value) {
        return H.Format.resolve(value);
    }
}
