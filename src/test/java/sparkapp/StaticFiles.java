package sparkapp;

import static act.boot.spark.SparkApp.*;

/**
 * Demonstrate how to use the static and external file location API
 *
 * @see {@link SparkApp#staticFileLocation(String)}
 * @see {@link SparkApp#staticFileLocation(String, String)}
 * @see {@link SparkApp#externalFileLocation(String)}
 * @see {@link SparkApp#externalFileLocation(String, String)}
 */
public class StaticFiles {
    public static void main(String[] args) {
        staticFileLocation("/public");
        staticFileLocation("/open", "/public");
        externalFileLocation("/tmp");
        externalFileLocation("/home", "/home/luog");
        before("/home/\\.ssh/.*", forbidden());
    }
}
