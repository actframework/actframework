package act.validation;

import act.conf.AppConfig;
import act.i18n.I18n;
import act.util.DestroyableBase;
import org.osgl.util.S;

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
        return interpolate(messageTemplate, context, I18n.locale());
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        if (messageTemplate.startsWith("{act.")) {
            return actInterpolate(messageTemplate, locale);
        }
        if (!messageTemplate.startsWith("{")) {
            messageTemplate = S.concat("{", messageTemplate, "}");
        }
        return realInterpolator.interpolate(messageTemplate, context, locale);
    }

    private String actInterpolate(String messageTemplate, Locale locale) {
        if (null == locale) {
            locale = I18n.locale();
        }
        return I18n.i18n(locale, "act_message", messageTemplate);
    }

}
