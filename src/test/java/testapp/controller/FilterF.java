package testapp.controller;

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.Finally;
import org.osgl.mvc.annotation.With;
import testapp.util.Trackable;

@With(FilterWithNoEffect.class)
public abstract class FilterF extends Trackable {

    @Finally(priority = 10)
    public static void f1() {
        trackStatic("FilterF", "f1");
    }

    @After(priority = 3)
    public static void afterP3() {
        trackStatic("FilterF", "afterP3");
    }
}
