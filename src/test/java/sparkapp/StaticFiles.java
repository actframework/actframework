package sparkapp;

import org.osgl.oms.boot.SparkApp;

import static org.osgl.oms.boot.SparkApp.*;
import static org.osgl.oms.controller.Controller.Util.*;

/**
 * Demonstrate how to use the static and external file location API
 *
 * @see {@link org.osgl.oms.boot.SparkApp#staticFileLocation(String)}
 * @see {@link org.osgl.oms.boot.SparkApp#staticFileLocation(String, String)}
 * @see {@link org.osgl.oms.boot.SparkApp#externalFileLocation(String)}
 * @see {@link org.osgl.oms.boot.SparkApp#externalFileLocation(String, String)}
 */
public class StaticFiles {
    public static void main(String[] args) {
        staticFileLocation("/public");
        staticFileLocation("/open", "/public");
        externalFileLocation("/tmp");
        externalFileLocation("/home", "/home/luog");
        before("/home/\\.ssh/.*", SparkApp.forbidden());
    }
}
