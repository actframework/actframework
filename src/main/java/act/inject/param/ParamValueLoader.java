package act.inject.param;

import act.util.ActContext;

/**
 * Responsible for providing the value to a method parameter
 */
interface ParamValueLoader {

    /**
     * Provide the value for a parameter from current execution context.
     *
     * The context could be one of
     *
     * * {@link act.app.ActionContext}
     * * {@link act.app.CliContext}
     *
     * @param bean    the bean that cached for current session or `null` if not applied
     * @param context the current execution context
     * @param noDefaultValue if `true` then it shall not load default value when not provided by request
     * @return the value object
     */
    Object load(Object bean, ActContext<?> context, boolean noDefaultValue);
}
