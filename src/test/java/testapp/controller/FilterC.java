package testapp.controller;

import org.osgl.mvc.annotation.Catch;
import org.osgl.mvc.annotation.With;

/**
 * Created by luog on 1/03/2015.
 */
@With(FilterF.class)
public class FilterC {

    @Catch
    public void onException() {

    }

    @Catch(except = "foo")
    public void onExceptionExceptFoo() {

    }
}
