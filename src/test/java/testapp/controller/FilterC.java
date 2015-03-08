package testapp.controller;

import org.osgl.mvc.annotation.Catch;
import org.osgl.mvc.annotation.With;
import testapp.util.Trackable;

/**
 * Created by luog on 1/03/2015.
 */
@With(FilterF.class)
public class FilterC extends Trackable {

    @Catch
    public void onException() {
        track("onException");
    }

    @Catch(except = "foo")
    public void onExceptionExceptFoo() {
        track("onExceptionExceptFoo");
    }
}
