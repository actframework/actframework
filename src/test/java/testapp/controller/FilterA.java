package testapp.controller;

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.With;

/**
 * Created by luog on 1/03/2015.
 */
@With(FilterB.class)
public class FilterA {

    @After
    public void after() {

    }

    @After(priority = 10)
    public void afterP10() {

    }

}
