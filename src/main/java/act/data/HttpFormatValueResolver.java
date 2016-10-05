package act.data;

import org.osgl.http.H;

public class HttpFormatValueResolver extends StringValueResolverPlugin<H.Format> {

    @Override
    public H.Format resolve(String value) {
        return H.Format.resolve(value);
    }
}
