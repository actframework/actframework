package act.util;

import act.app.ActionContext;
import org.osgl.http.HttpConfig;

import java.util.Locale;

/**
 * Interface for web-based locale resolution strategies that allows for
 * locale resolution via the {@link ActionContext}.
 */
public interface LocaleResolver {
    Locale resolve(ActionContext context);

    public enum impl {
        ;
        public static LocaleResolver DEFAULT = new LocaleResolver() {
            @Override
            public Locale resolve(ActionContext context) {
                Locale locale = context.req().locale();
                if (locale == HttpConfig.defaultLocale()) {
                    locale = context.config().locale();
                }
                return locale;
            }
        };
    }
}
