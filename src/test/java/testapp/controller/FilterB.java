package testapp.controller;

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.With;
import testapp.util.Trackable;

/**
 * Created by luog on 1/03/2015.
 */
@With({FilterWithNoEffect.class, FilterF.class})
public class FilterB extends Trackable {

    @Before
    public static void before() {
        trackStatic("FilterB", "before");
    }

    @Before(except = "foo")
    public void beforeExceptFoo() {
        track("beforeExceptFoo");
    }

    @Before(except = "foo, bar")
    public void beforeExceptFooBar() {
        track("beforeExceptFooBar");
    }

    @Before(except = {"foo", "bar"})
    public void beforeExceptFooBar2() {
        track("beforeExceptFooBar2");
    }

    @Before(only = "foo")
    public void beforeOnlyFoo() {
        track("beforeOnlyFoo");
    }

    @Before(only = "foo, bar")
    public void beforeOnlyFooBar() {
        track("beforeOnlyFooBar");
    }

    @After(priority = 99)
    public void afterP99() {
        track("afterP99");
    }
}
