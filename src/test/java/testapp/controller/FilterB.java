package testapp.controller;

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.With;

/**
 * Created by luog on 1/03/2015.
 */
@With({FilterWithNoEffect.class, FilterF.class})
public class FilterB {

    @Before
    public static void before() {

    }

    @Before(except = "foo")
    public void beforeExceptFoo() {

    }

    @Before(except = "foo, bar")
    public void beforeExceptFooBar() {

    }

    @Before(except = {"foo", "bar"})
    public void beforeExceptFooBar2() {

    }

    @Before(only = "foo")
    public void beforeOnlyFoo() {

    }

    @Before(only = "foo, bar")
    public void beforeOnlyFooBar() {

    }

    @After(priority = 99)
    public void afterP99() {

    }
}
