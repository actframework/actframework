package act;

import org.osgl.util.E;
import org.osgl.util.S;

import java.io.IOException;
import java.util.Properties;

/**
 * Stores the act version and build number
 */
class Version {
    private static String version;
    private static String buildNumber;

    static {
        Properties p = new Properties();
        try {
            p.load(Version.class.getResourceAsStream("/act.version"));
            version = p.getProperty("version");
            if (version.endsWith("SNAPSHOT")) {
                version = version.replace("SNAPSHOT", "S");
            } else {
                version = "R" + version;
            }
            buildNumber = p.getProperty("build");
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    static String version() {
        return version;
    }

    static String buildNumber() {
        return buildNumber;
    }

    static String fullVersion() {
        return S.fmt("%s-%s", version, buildNumber);
    }
}
