package act.util;

import act.Act;
import act.app.App;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.FastStr;

/**
 * Utility class to lookup/build a URL
 */
public class UrlBuilder {
    private Protocol protocol;
    private String host;
    private int port;
    private String path;

    public UrlBuilder() {
        this.protocol = defaultProtocol();
        this.port = defaultPort();
        this.host = defaultHost();
        this.path = "";
    }

    public UrlBuilder protocol(String protocol) {
        this.protocol = Protocol.lookup(protocol);
        if (null == this.protocol) {
            E.illegalArgumentIf(true, "Unknown protocol: %s", protocol);
        }
        return this;
    }

    public UrlBuilder protocol(Protocol protocol) {
        this.protocol = $.notNull(protocol);
        return this;
    }

    public Protocol protocol() {
        return this.protocol;
    }

    public UrlBuilder host(String host) {
        this.host = host.trim();
        return this;
    }

    public String host() {
        return this.host;
    }

    public UrlBuilder port(int port) {
        this.port = port;
        return this;
    }

    public int port() {
        return this.port;
    }

    public UrlBuilder path(String path) {
        this.path = path.trim();
        return this;
    }

    public String path() {
        return path;
    }

    public static UrlBuilder parse(String url) {
        UrlBuilder builder = new UrlBuilder();
        FastStr fs = FastStr.of(url);
        FastStr part = fs.before("://");
        if (FastStr.EMPTY_STR != part) {
            builder.protocol(Protocol.lookup(part.toString()));
            fs = fs.afterFirst("://");
        }
        part = fs.beforeFirst(":");
        if (FastStr.EMPTY_STR != part) {
            builder.host(part.toString());
            part = fs.afterFirst(":").beforeFirst("/");
            if (FastStr.EMPTY_STR == part) {
                throw new InvalidUrlException(url);
            }
            builder.port(Integer.parseInt(part.toString()));
            fs = fs.afterFirst(part);
        }
        if (!fs.startsWith("/")) {
            fs = fs.prepend("/");
        }
        builder.path(fs.toString());
        return builder;
    }

    private static Protocol defaultProtocol() {
        App app = Act.app();
        if (null != app) {
            boolean secure = app.config().httpSecure();
            return secure ? Protocol.HTTPS : Protocol.HTTP;
        } else {
            return Act.isDev() ? Protocol.HTTP : Protocol.HTTPS;
        }
    }

    private static int defaultPort() {
        App app = Act.app();
        if (null != app) {
            // running inside an Act application
            return app.config().httpPort();
        } else {
            // running as in a standard Java program
            return Act.isDev() ? 80 : 443;
        }
    }

    private static String defaultHost() {
        App app = Act.app();
        if (null != app) {
            // running inside an Act application
            return app.config().host();
        } else {
            // return the localhost
            return "localhost";
        }
    }

}
