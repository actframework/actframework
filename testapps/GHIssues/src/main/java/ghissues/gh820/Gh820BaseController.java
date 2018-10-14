package ghissues.gh820;

import ghissues.BaseController;

import javax.inject.Inject;

public class Gh820BaseController<T, SERVICE extends BaseService<T>> extends BaseController {
    @Inject
    private SERVICE service;

    protected SERVICE service() {
        return service;
    }
}
