package act;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.io.IOException;
import java.net.URL;
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

    public synchronized static String appVersion(String appName) {
        final String UNKNOWN = "unknown";
        if (null == appVersion) {
            URL url = appVersionData(appName);
            if (null != url) {
                try {
                    Properties p = IO.loadProperties(IO.is(url));
                    String version = p.getProperty("version");
                    String buildNum = p.getProperty("build");
                    if (S.blank(version)) {
                        appVersion = S.blank(buildNum) ? UNKNOWN : buildNum;
                    } else {
                        appVersion = S.blank(buildNum) ? version : version + '-' + buildNum;
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        if (null == appVersion) {
            appVersion = UNKNOWN;
        }
        return appVersion;
    }

    private static URL appVersionData(String appName) {
        String path = "/" + Keyword.of(appName).dashed() + ".version";
        URL url = Version.class.getResource(path);
        if (null == url) {
            url = Version.class.getResource("/app.version");
        }
        return url;
    }

}
