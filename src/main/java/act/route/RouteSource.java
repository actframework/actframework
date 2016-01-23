package act.route;

/**
 * Enumerate the source where the route mapping come from.
 * <p>
 *     There are three possible route mapping source:
 * </p>
 * <ul>
 *     <li>
 *         route table - the route mapping defined in a route file
 *     </li>
 *     <li>
 *         action annotation - the route mapping defined by action handler method
 *         along with action annotation
 *     </li>
 *     <li>
 *         app config - route mapping configured by code by calling
 *         {@link act.app.conf.AppConfigurator.RouteBuilder} APIs
 *     </li>
 * </ul>
 */
public enum RouteSource {

    BUILD_IN() {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            switch (existingRoute) {
                case BUILD_IN:
                    return Router.ConflictResolver.EXIT;
                default:
                    return Router.ConflictResolver.SKIP;
            }
        }
    },

    /**
     * The route mapping from route table file
     */
    ROUTE_TABLE() {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            switch (existingRoute) {
                case APP_CONFIG:
                    return Router.ConflictResolver.EXIT;
                case ACTION_ANNOTATION:
                    return Router.ConflictResolver.OVERWRITE;
                default:
                    return Router.ConflictResolver.OVERWRITE_WARN;
            }
        }
    },

    /**
     * The route mapping come from action annotation
     */
    ACTION_ANNOTATION() {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            switch (existingRoute) {
                case ACTION_ANNOTATION:
                    return Router.ConflictResolver.EXIT;
                case ROUTE_TABLE:
                case APP_CONFIG:
                    return Router.ConflictResolver.SKIP;
                default:
                    return Router.ConflictResolver.OVERWRITE_WARN;
            }
        }
    },

    /**
     * The route mapping from programed configuration
     */
    APP_CONFIG() {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            switch (existingRoute) {
                case APP_CONFIG:
                    return Router.ConflictResolver.EXIT;
                default:
                    return Router.ConflictResolver.OVERWRITE_WARN;
            }
        }
    };

    abstract Router.ConflictResolver onConflict(RouteSource existingRoute);
}
