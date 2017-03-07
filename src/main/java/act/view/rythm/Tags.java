package act.view.rythm;

import act.Act;
import act.app.ActionContext;
import act.i18n.I18n;
import act.route.Router;
import act.util.ActContext;
import org.osgl.util.E;
import org.rythmengine.RythmEngine;
import org.rythmengine.template.JavaTagBase;
import org.rythmengine.utils.S;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines fast tags for Act app
 */
public class Tags {

    @Inject
    private List<JavaTagBase> fastTags;

    public void register(RythmEngine engine) {
        for (JavaTagBase tag : fastTags) {
            engine.registerFastTag(tag);
        }
    }

    /**
     * Retrieve validation error message by field name
     *
     * Usage: `@_error("foo.name")` where`foo.name` is the field name
     */
    public static class ValidationError extends JavaTagBase {
        @Override
        public String __getName() {
            return "_error";
        }

        @Override
        protected void call(__ParameterList params, __Body body) {
            int paramSize = params.size();
            E.illegalArgumentIf(paramSize < 1);
            String field = params.get(0).value.toString();
            ConstraintViolation violation = ActContext.Base.currentContext().violation(field);
            if (null != violation) {
                p(violation.getMessage());
            }
        }
    }

    /**
     * Retrieve act framework defined i18n messages
     *
     * Usage: `@actMsg("msg-id")`
     */
    public static class ActMessage extends JavaTagBase {
        @Override
        public String __getName() {
            return "actMsg";
        }

        @Override
        protected void call(__ParameterList params, __Body body) {
            int paramSize = params.size();
            E.illegalArgumentIf(paramSize < 1);
            String msg = params.get(0).value.toString();
            Object[] args;
            if (paramSize > 1) {
                args = new Object[paramSize - 1];
                for (int i = 1; i < paramSize; ++i) {
                    args[i - 1] = params.get(i).value;
                }
            } else {
                args = new Object[0];
            }
            p(I18n.i18n(I18n.locale(), "act_message", msg, args));
        }
    }

    /**
     * Retrieve reverse routing URL path
     *
     * Usage: `@fullUrl()`
     */
    public static class ReverseRouting extends JavaTagBase {

        private boolean fullUrl = false;

        public ReverseRouting() {
        }

        protected ReverseRouting(boolean fullUrl) {
            this.fullUrl = fullUrl;
        }

        @Override
        public String __getName() {
            return "url";
        }

        @Override
        protected void call(__ParameterList parameterList, __Body body) {
            Object o = parameterList.getByName("value");
            if (null == o) {
                o = parameterList.getDefault();
            }
            String value = o.toString();

            boolean fullUrl = this.fullUrl;
            o = parameterList.getByName("fullUrl");
            if (null != o) {
                fullUrl = (Boolean) o;
            }

            ActionContext context = ActionContext.current();
            Router router = null == context ? Act.app().router() : context.router();

            if (value.contains("/") || !value.contains(".")) {
                int n = parameterList.size();
                Object[] args = new Object[n - 1];
                for (int i = 0; i < n - 1; ++i) {
                    args[i] = parameterList.getByPosition(i + 1);
                }
                if (fullUrl) {
                    p(router._fullUrl(value, args));
                } else {
                    p(String.format(value, args));
                }
            } else {
                // value is an action path, need reverse route
                Map<String, Object> args = new HashMap<>();
                for (__Parameter param : parameterList) {
                    String name = param.name;
                    if (S.ne(name, "value") && S.ne(name, "fullUrl")) {
                        args.put(param.name, param.value);
                    }
                }

                p(router.reverseRoute(value, args, fullUrl));
            }
        }
    }

    public static class FullUrl extends ReverseRouting {
        @Override
        public String __getName() {
            return "fullUrl";
        }

        public FullUrl() {
            super(true);
        }
    }

}
