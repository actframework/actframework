package testapp.controller;

import act.app.ActionContext;

public class ContextController extends ControllerBase {
    protected ActionContext ctx;
    public void setAppContext(ActionContext ctx) {
        this.ctx = ctx;
    }
}
