package act.di.feather;

import act.app.ActionContext;
import act.app.App;
import act.app.CliContext;
import act.app.util.AppCrypto;
import act.conf.AppConfig;
import act.di.DependencyInjectionBinder;
import act.di.DependencyInjector;
import act.di.DependencyInjectorBase;
import act.event.EventBus;
import act.mail.MailerContext;
import act.util.ActContext;
import org.osgl.$;

import java.util.Map;

public class FeatherInjector extends DependencyInjectorBase<FeatherInjector> {

    private volatile Feather feather;
    public FeatherInjector(App app) {
        super(app);
    }

    @Override
    public <T> T create(Class<T> clazz) {
        Feather feather = feather();
        if (null == feather) {
            return $.newInstance(clazz);
        } else {
            return feather.instance(clazz);
        }
    }

    @Override
    public DependencyInjector<FeatherInjector> createContextAwareInjector(ActContext context) {
        return this;
    }

    private Feather feather() {
        if (null == feather) {
            synchronized (this) {
                if (null == feather) {
                    feather = Feather.with(this).dependencyInjector(this);
                    for (Map.Entry<Class, DependencyInjectionBinder> entry : binders.entrySet()) {
                        feather.registerProvider(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return feather;
    }

    @Override
    public synchronized void registerDiBinder(DependencyInjectionBinder binder) {
        super.registerDiBinder(binder);
        feather = null; // reset feather
    }

    // -- following are ACT specific provider methods
    @Provides
    public App app() {
        return App.instance();
    }

    @Provides
    public AppConfig appConfig() {
        return app().config();
    }

    @Provides
    public AppCrypto appCrypto() {
        return app().crypto();
    }

    @Provides
    public ActionContext actionContext() {
        return ActionContext.current();
    }

    @Provides
    public MailerContext mailerContext() {
        return MailerContext.current();
    }

    @Provides
    public CliContext cliContext() {
        return CliContext.current();
    }

    @Provides
    public EventBus eventBus() {
        return app().eventBus();
    }
}
