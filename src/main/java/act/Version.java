package act;

import org.osgl.util.E;
import org.osgl.util.S;

import java.io.IOException;
import java.util.Properties;

/**
 * Stores the act version and build number
 */
public class Version {
    private static boolean snapshot;
    private static String version;
    private static String buildNumber;
    private static String appVersion;

    static {
        Properties p = new Properties();
        try {
            p.load(Version.class.getResourceAsStream("/act.version"));
            version = p.getProperty("version");
            if (version.endsWith("SNAPSHOT")) {
                version = "v" + version.replace("-SNAPSHOT", "");
                snapshot = true;
            } else {
                version = "R" + version;
            }
            buildNumber = p.getProperty("build");
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static boolean snapshot() {
        return snapshot;
    }

    public static String version() {
        return version;
    }

    public static String buildNumber() {
        return buildNumber;
    }

    public static String fullVersion() {
        return S.fmt("%s-%s", version, buildNumber);
    }

    public synchronized static String appVersion() {
        if (null == appVersion) {
            Properties p = new Properties();
            try {
                p.load(Version.class.getResourceAsStream("/app.version"));
                appVersion = p.getProperty("app.version");
            } catch (Exception e) {
                // ignore
                appVersion = "0.0.1";
            }
        }
        return appVersion;
    }

}
