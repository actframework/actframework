package testapp.controller;

public abstract class HandlerEnhancerTestController extends Controller {

    public String className() {
        return getClass().getName();
    }

}
