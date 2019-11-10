package act.route;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
 * </ul>
 */
public enum RouteSource {

    BUILD_IN("Built in service") {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            return Router.ConflictResolver.EXIT;
        }
    },

    /**
     * The route mapping from route table file
     */
    ROUTE_TABLE("Route table") {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            switch (existingRoute) {
                case APP_CONFIG:
                case BUILD_IN:
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
    ACTION_ANNOTATION("Action annotation") {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            switch (existingRoute) {
                case ACTION_ANNOTATION:
                case BUILD_IN:
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
    APP_CONFIG("App configured") {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            switch (existingRoute) {
                case BUILD_IN:
                case APP_CONFIG:
                    return Router.ConflictResolver.EXIT;
                default:
                    return Router.ConflictResolver.OVERWRITE_WARN;
            }
        }
    },

    /**
     * The route mapping com from admin console instruction
     */
    ADMIN_OVERWRITE("Admin overwrite") {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            return Router.ConflictResolver.OVERWRITE_WARN;
        }
    },

    /**
     * The route mapping com from admin console add instruction
     */
    ADMIN_ADD("Admin added") {
        @Override
        Router.ConflictResolver onConflict(RouteSource existingRoute) {
            return Router.ConflictResolver.EXIT;
        }
    },
    ;

    private String desc;

    RouteSource(String desc) {
        this.desc = desc;
    }

    public String getDescription() {
        return desc;
    }

    abstract Router.ConflictResolver onConflict(RouteSource existingRoute);
}
