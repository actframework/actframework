package testapp.controller;

import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import playground.EmailBinder;
import playground.MyConstraint;

public class ParamWithAnnotationController {
    @Action(value = "/foo", methods = {H.Method.POST, H.Method.GET})
    public void bindNameChanged(@Param("bar") String foo) {}

    @Action("/x")
    public void defValPresented(@Param(defIntVal = 5) int x) {}

    @Action("/y")
    public void binderRequired(@Bind(EmailBinder.class) @MyConstraint(groups = {String.class, Integer.class}) String email) {}

}
