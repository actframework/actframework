package testapp.controller;

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.Finally;
import org.osgl.mvc.annotation.With;

@With(FilterWithNoEffect.class)
public abstract class FilterF {

    @Finally(priority = 10)
    public static void f1() {
    }

    @After(priority = 3)
    public static void afterP3() {

    }
}
