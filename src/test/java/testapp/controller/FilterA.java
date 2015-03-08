package testapp.controller;

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.With;
import testapp.util.Trackable;

/**
 * Created by luog on 1/03/2015.
 */
@With(FilterB.class)
public class FilterA extends Trackable {

    @After
    public void after() {
        track("after");
    }

    @After(priority = 10)
    public void afterP10() {
        track("afterP10");
    }

}
