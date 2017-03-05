package act.validation;

import act.conf.AppConfig;
import act.util.ActContext;
import act.util.DestroyableBase;

import javax.validation.MessageInterpolator;
import java.util.Locale;

public class ActValidationMessageInterpolator extends DestroyableBase implements MessageInterpolator {

    private MessageInterpolator realInterpolator;
    private AppConfig config;

    public ActValidationMessageInterpolator(MessageInterpolator defaultInterpolator, AppConfig config) {
        this.realInterpolator = defaultInterpolator;
        this.config = config;
    }

    @Override
    public String interpolate(String messageTemplate, Context context) {
        ActContext actContext = ActContext.Base.currentContext();
        Locale locale = null == actContext ? config.locale() : actContext.locale(true);
        return interpolate(messageTemplate, context, locale);
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        return realInterpolator.interpolate(messageTemplate, context, locale);
    }
}
