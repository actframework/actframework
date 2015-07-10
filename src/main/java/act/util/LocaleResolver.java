package act.util;

import act.app.AppContext;

import java.util.Locale;

/**
 * Interface for web-based locale resolution strategies that allows for
 * locale resolution via the {@link AppContext}.
 */
public interface LocaleResolver {
    Locale resolve(AppContext context);

    public enum impl {
        ;
        public static LocaleResolver DEFAULT = new LocaleResolver() {
            @Override
            public Locale resolve(AppContext context) {
                return context.req().locale();
            }
        };
    }
}
