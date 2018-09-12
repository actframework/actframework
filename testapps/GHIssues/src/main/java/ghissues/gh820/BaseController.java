package ghissues.gh820;

import act.util.LogSupport;

import javax.inject.Inject;

public class BaseController<T, SERVICE extends BaseService<T>> extends LogSupport {
    @Inject
    private SERVICE service;

    protected SERVICE service() {
        return service;
    }
}
