package act.util;

import org.osgl.util.S;

/**
 * The exception indicate a argument is unknown {@link Protocol}
 */
public class UnknownProtocolException extends IllegalArgumentException {
    private String protocol;

    public UnknownProtocolException(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getMessage() {
        return S.fmt("Unknown protocol: %s", protocol);
    }

    public String getProtocol() {
        return protocol;
    }
}
