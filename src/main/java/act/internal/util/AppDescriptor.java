package act.internal.util;

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

import act.Act;
import org.osgl.$;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;
import osgl.version.Version;

import java.io.*;

/**
 * Describe an application's name, package and version
 */
public class AppDescriptor implements Serializable {

    private static final Logger LOGGER = Act.LOGGER;

    private String appName;
    private String packageName;
    private Version version;
    private long start = $.ms();

    /**
     * Construct an `AppVersion` with name and version
     *
     * @param appName
     *      the app name
     * @param version
     *      the app version
     */
    private AppDescriptor(String appName, String packageName, Version version) {
        if (appName.startsWith("${")) {
            LOGGER.warn("app name is substitute variable - fallback to default app name");
            appName = S.afterLast(packageName, ".");
        }
        this.appName = appName;
        this.packageName = packageName;
        this.version = version;
    }

    /**
     * Returns the app name
     * @return
     *      the app name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Returns the package name
     * @return
     *      the package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Returns the app version
     * @return
     *      the app version
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Check if this `AppVersion` data is valid.
     *
     * An `AppVersion` is considered to be valid if and only if
     * the {@link #getVersion()} is **not** {@link Version#isUnknown() unknown}
     *
     * @return
     *      `true` if this `AppVersion` is valid or `false` otherwise
     */
    public boolean isValid() {
        return !version.isUnknown();
    }

    public long getStart() {
        return this.start;
    }

    /**
     * Serialize this `AppDescriptor` and output byte array
     *
     * @return
     *      serialize this `AppDescriptor` into byte array
     */
    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            return baos.toByteArray();
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    // - factory methods

    /**
     * Create an `AppDescriptor` with appName, package name and app version.
     *
     * If `appName` is `null` or blank, it will try the following
     * approach to get app name:
     *
     * 1. check the {@link Version#getArtifactId() artifact id} and use it unless
     * 2. if artifact id is null or empty, then infer app name using {@link AppNameInferer}
     *
     * @param appName
     *      the app name, optional
     * @param packageName
     *      the package name
     * @param appVersion
     *      the app version
     * @return
     *      an `AppDescriptor`
     */
    public static AppDescriptor of(String appName, String packageName, Version appVersion) {
        String[] packages = packageName.split(S.COMMON_SEP);
        String effectPackageName = packageName;
        if (packages.length > 0) {
            effectPackageName = packages[0];
        }
        E.illegalArgumentIf(!JavaNames.isPackageOrClassName(effectPackageName),
                "valid package name expected. found: " + effectPackageName);
        return new AppDescriptor(ensureAppName(appName, effectPackageName, $.requireNotNull(appVersion)),
                packageName,
                appVersion);
    }

    /**
     * Create an `AppDescriptor` with appName, entry class and app version.
     *
     * If `appName` is `null` or blank, it will try the following
     * approach to get app name:
     *
     * 1. check the {@link Version#getArtifactId() artifact id} and use it unless
     * 2. if artifact id is null or empty, then infer app name using {@link AppNameInferer}
     *
     * @param appName
     *      the app name, optional
     * @param entryClass
     *      the entry class
     * @param appVersion
     *      the app version
     * @return
     *      an `AppDescriptor`
     */
    public static AppDescriptor of(String appName, Class<?> entryClass, Version appVersion) {
        return new AppDescriptor(ensureAppName(appName, entryClass, $.requireNotNull(appVersion)),
                JavaNames.packageNameOf(entryClass),
                appVersion);
    }

    /**
     * Create an `AppDescriptor` with appName and entry class specified.
     *
     * If `appName` is `null` or blank, it will try the following
     * approach to get app name:
     *
     * 1. check the {@link Version#getArtifactId() artifact id} and use it unless
     * 2. if artifact id is null or empty, then infer app name using {@link AppNameInferer}
     *
     * @param appName
     *      the app name
     * @param entryClass
     *      the entry class
     * @return
     *      an `AppDescriptor` instance
     */
    public static AppDescriptor of(String appName, Class<?> entryClass) {
        System.setProperty("osgl.version.suppress-var-found-warning", "true");
        return of(appName, entryClass, Version.of(entryClass));
    }

    /**
     * Create an `AppDescriptor` with appName and package name specified
     *
     * If `appName` is `null` or blank, it will try the following
     * approach to get app name:
     *
     * 1. check the {@link Version#getArtifactId() artifact id} and use it unless
     * 2. if artifact id is null or empty, then infer app name using {@link AppNameInferer}
     *
     * @param appName
     *      the app name
     * @param packageName
     *      the package name of the app
     * @return
     *      an `AppDescriptor` instance
     */
    public static AppDescriptor of(String appName, String packageName) {
        String[] packages = packageName.split(S.COMMON_SEP);
        return of(appName, packageName, Version.ofPackage(packages[0]));
    }

    /**
     * Create an `AppDescriptor` with package name.
     *
     * This method relies on {@link Version#ofPackage(String)} to get
     * the corresponding {@link Version} instance to the package name.
     *
     * @param packageName
     *      the package name
     * @return
     *      an `AppDescriptor` instance
     */
    public static AppDescriptor of(String packageName) {
        return of(null, packageName);
    }

    /**
     * Create an `AppDescriptor` with entry class (the class with main method)
     *
     * This method relies on {@link Version#of(Class)} to get the
     * corresponding {@link Version} instance to the entry class
     *
     * @param entryClass
     *      the entry class
     * @return
     *      An `AppDescriptor` instance
     */
    public static AppDescriptor of(Class<?> entryClass) {
        return of(null, entryClass);
    }

    /**
     * Deserialize an `AppDescriptor` from byte array
     *
     * @param bytes
     *      the byte array
     * @return
     *      an `AppDescriptor` instance
     */
    public static AppDescriptor deserializeFrom(byte[] bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (AppDescriptor) ois.readObject();
        } catch (IOException e) {
            throw E.ioException(e);
        } catch (ClassNotFoundException e) {
            throw E.unexpected(e);
        }
    }

    private static String ensureAppName(String appName, Class<?> entryClass, Version version) {
        if (S.blank(appName)) {
            if (!version.isUnknown()) {
                appName = version.getArtifactId();
            }
            if (S.blank(appName)) {
                appName = AppNameInferer.from(entryClass);
            }
        }
        return appName;
    }

    private static String ensureAppName(String appName, String packageName, Version version) {
        if (S.blank(appName)) {
            if (!version.isUnknown()) {
                appName = version.getArtifactId();
            }
            if (S.blank(appName)) {
                appName = AppNameInferer.fromPackageName(packageName);
            }
        }
        return appName;
    }
}
