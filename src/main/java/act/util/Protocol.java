package act.util;

import org.osgl.util.C;

import java.util.Map;

/**
 * Recognized URL protocols
 */
public enum Protocol {
    HTTP,
    HTTPS,
    WS,
    WSS,
    FTP,
    SFTP;

    private static Map<String, Protocol> cache;
    private synchronized static Map<String, Protocol> cache() {
        if (null == cache) {
            cache = C.newMap();
            for (Protocol p : values()) {
                cache.put(p.name(), p);
            }
        }
        return cache;
    }
    public String toString() {
        return name().toLowerCase();
    }

    /**
     * Return a `Protocol` instance based on the `protocolName` specified. The lookup
     * process is:
     *
     * 1. trim prefix/suffix spaces from the `protocolName`
     * 2. convert the `protocolName` to upper case
     * 3. look up Protocol instances throw {@link #values()} to match the {@link #name()} with processed string
     *
     * If any match found then that protocol instance is returned. Otherwise the
     * {@link UnknownProtocolException} will be thrown out
     *
     * @param protocolName the name used to lookup protocol instance
     * @return the protocol matches the name as per process described
     * @throws UnknownProtocolException if no protocol matches the name
     */
    public static Protocol lookup(String protocolName) throws UnknownProtocolException {
        Protocol protocol = cache().get(protocolName.trim().toUpperCase());
        if (null == protocol) {
            throw new UnknownProtocolException(protocolName);
        }
        return protocol;
    }
}
