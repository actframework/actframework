package testapp.controller;

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.With;
import testapp.util.Trackable;

public class FilterAB extends FilterA {

    @After
    public void after() {
        track("after");
    }

    @After(priority = 10)
    public void afterP10() {
        track("afterP10");
    }

}
