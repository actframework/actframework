package act.di.param;

import act.util.ActContext;

/**
 * Responsible for providing the value to a method parameter
 */
public interface ParamValueLoader {
    /**
     * Provide the value for a parameter from current execution context.
     *
     * The context could be one of
     *
     * * {@link act.app.ActionContext}
     * * {@link act.mail.MailerContext}
     * * {@link act.app.CliContext}
     *
     * @param context the current execution context
     * @return the value object
     */
    Object load(ActContext context);

    /**
     * Provide the value for a parameter from current execution context.
     * @param context the current execution context
     * @param noDefaultValue if `true` then it shall not load default value when not provided by request
     * @return the value object
     *
     * @see #load(ActContext)
     */
    Object load(ActContext context, boolean noDefaultValue);
}
