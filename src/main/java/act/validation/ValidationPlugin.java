package act.validation;

import act.Act;
import act.app.App;
import act.inject.ActProvider;
import act.plugin.AppServicePlugin;

import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class ValidationPlugin extends AppServicePlugin {

    private Configuration config;
    private ValidatorFactory factory;
    private Validator validator;

    @Override
    protected void applyTo(App app) {
        init(app);
        app.registerSingleton(ValidationPlugin.class, this);
    }

    private void init(App app) {
        config = Validation.byDefaultProvider().configure();
        config.messageInterpolator(new ActValidationMessageInterpolator(config.getDefaultMessageInterpolator(), app.config()));
        ensureFactoryValidator();
    }

    private void ensureFactoryValidator() {
        if (validator != null) {
            return;
        }
        if (factory == null) {
            factory = config.buildValidatorFactory();
        }
        validator = factory.getValidator();
    }

    public static class ValidatorProvider extends ActProvider<Validator> {
        @Override
        public Validator get() {
            ValidationPlugin plugin = Act.singleton(ValidationPlugin.class);
            return plugin.validator;
        }
    }

}
