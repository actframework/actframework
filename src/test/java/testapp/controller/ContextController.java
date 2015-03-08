package testapp.controller;

import org.osgl.oms.app.AppContext;

public class ContextController extends Controller {
    protected AppContext ctx;
    public void setAppContext(AppContext ctx) {
        this.ctx = ctx;
    }
}
