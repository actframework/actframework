package com.mycom.gh1322;

import act.Act;
import act.metric.MeasureCount;
import act.metric.MeasureTime;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;
import osgl.version.Version;
import osgl.version.Versioned;

/**
 * A simple hello world app entry
 *
 * Run this app, try to update some of the code, then
 * press F5 in the browser to watch the immediate change
 * in the browser!
 */
@SuppressWarnings("unused")
@Versioned
public class AppEntry {

    /**
     * Version of this application
     */
    public static final Version VERSION = Version.of(AppEntry.class);

    /**
     * A logger instance that could be used throughout the application
     */
    public static final Logger LOGGER = LogManager.get(AppEntry.class);

    @GetAction
    public String getFoo() {
        return foo();
    }

    @MeasureCount
    @MeasureTime
    private String foo() {
        return S.random();
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }

}
