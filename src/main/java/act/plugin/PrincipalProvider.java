package act.plugin;

import act.app.ActionContext;

import javax.inject.Provider;

/**
 * Provide the current user principal name.
 */
public interface PrincipalProvider extends Provider<String> {

    /**
     * The default implementation of {@link PrincipalProvider}.
     *
     * It fetch the principal name from {@link ActionContext#username()}
     */
    class DefaultPrincipalProvider implements PrincipalProvider {

        public static final PrincipalProvider INSTANCE = new DefaultPrincipalProvider();

        private DefaultPrincipalProvider() {}

        @Override
        public String get() {
            ActionContext context = ActionContext.current();
            return null == context ? null : context.username();
        }
    }
}
