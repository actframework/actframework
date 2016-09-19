package act.inject.param;

import act.Destroyable;
import act.app.ActionContext;
import act.app.App;
import act.app.AppServiceBase;
import act.cli.CliContext;
import act.util.ActContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage {@link ParamValueLoaderService} by context type
 */
@ApplicationScoped
public class ParamValueLoaderManager  extends AppServiceBase<ParamValueLoaderManager> {

    private final Map<Class<? extends ActContext>, ParamValueLoaderService> loaderServices = new HashMap<Class<? extends ActContext>, ParamValueLoaderService>();

    @Inject
    public ParamValueLoaderManager(App app) {
        super(app);
        loaderServices.put(ActionContext.class, new ActionContextParamLoader(app));
        loaderServices.put(CliContext.class, new CliContextParamLoader(app));
    }

    public <T extends ParamValueLoaderService> T  get(Class<? extends ActContext> contextClass) {
        return (T) loaderServices.get(contextClass);
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.destroyAll(loaderServices.values(), ApplicationScoped.class);
    }
}
