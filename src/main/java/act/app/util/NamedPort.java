package act.app.util;

import org.osgl.util.E;
import org.osgl.util.S;

public class NamedPort {
    private String name;
    private int port;

    public NamedPort(String name, int port) {
        E.NPE(name);
        E.illegalArgumentIf(port < 0);
        this.name = name;
        this.port = port;
    }

    public String name() {
        return name;
    }

    public int port() {
        return port;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NamedPort) {
            NamedPort that = (NamedPort) obj;
            return S.eq(that.name, name);
        }
        return false;
    }

    @Override
    public String toString() {
        return S.fmt("%s[:%s]", name, port);
    }
}
