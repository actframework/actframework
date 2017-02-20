package act.validation;

import act.conf.AppConfig;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import javax.validation.MessageInterpolator;
import javax.validation.metadata.ConstraintDescriptor;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ValidationMessageInterpolator extends DestroyableBase implements MessageInterpolator {

    public static final String APP_VALIDATION_MESSAGE = "appValidationMessages";

    private AppConfig appConfig;
    private Map<Locale, ResourceBundle> appBundleMap;
    private Map<Locale, ResourceBundle> defaultBundleMap;
    private ResourceBundle defaultAppBundle;
    private ResourceBundle defaultBundle;

    public ValidationMessageInterpolator(AppConfig config) {
        appConfig = $.notNull(config);
        appConfig.addSubResource(this);
        appBundleMap = C.newMap();
        defaultBundleMap = C.newMap();
        defaultAppBundle = getResourceBundle(APP_VALIDATION_MESSAGE, config.locale());
        defaultBundle = getResourceBundle("ValidationMessages", config.locale());
    }

    @Override
    protected void releaseResources() {
        super.releaseResources();
        appConfig = null;
        defaultBundleMap.clear();
        appBundleMap.clear();
    }

    @Override
    public String interpolate(String messageTemplate, Context context) {
        return interpolate(messageTemplate, context, null);
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        String resolvedTemplate = resolveTemplate(messageTemplate, locale);
        ConstraintDescriptor desc = context.getConstraintDescriptor();
        Map<String, Object> attrs = desc.getAttributes();
        Object val = context.getValidatedValue();
        return merge(resolvedTemplate, val, attrs);
    }

    private String merge(String template, Object val, Map<String, Object> attrs) {
        FastStr s = FastStr.of(template);
        S.Buffer sb = S.newBuffer();
        while (true) {
            int p0 = s.indexOf('$');
            if (p0 >= 0) {
                sb.append(s.subList(0, p0));
                s = s.substr(p0 + 1);
            }
            p0 = s.indexOf('{');
            if (p0 >= 0) {
                s = s.substr(p0 + 1);
            } else {
                sb.append(s);
                break;
            }
            int p1 = s.indexOf('}');
            String param = s.substring(0, p1);
            String paramVal = S.string(attrs.get(param));
            sb.append(paramVal);
            s = s.substr(p1 + 1);
        }
        return sb.length() == 0 ? template : sb.toString();
    }

    private String resolveTemplate(String messageTemplateId, Locale locale) {
        if (messageTemplateId.startsWith("$")) {
            messageTemplateId = messageTemplateId.substring(1);
        }
        if (messageTemplateId.startsWith("{")) {
            messageTemplateId = messageTemplateId.substring(1);
            messageTemplateId = messageTemplateId.substring(0, messageTemplateId.length() - 1);
        }
        ResourceBundle bundle = null == locale ? defaultAppBundle : appBundleMap.get(locale);
        if (null == bundle || !bundle.containsKey(messageTemplateId)) {
            bundle = null == locale ? defaultBundle : defaultBundleMap.get(locale);
        }
        if (null == bundle || !bundle.containsKey(messageTemplateId)) {
            return messageTemplateId;
        }
        return bundle.getString(messageTemplateId);
    }

    private static ResourceBundle getResourceBundle(String name, Locale locale) {
        try {
            return ResourceBundle.getBundle(name, locale);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public static void main(String[] args) {
    }

}
