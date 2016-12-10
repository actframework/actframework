package act.view.rythm;

import act.app.ActionContext;
import act.route.Router;
import org.osgl.inject.annotation.TypeOf;
import org.rythmengine.RythmEngine;
import org.rythmengine.template.JavaTagBase;
import org.rythmengine.utils.S;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines fast tags for Act app
 */
public class Tags {

    @Inject
    @TypeOf
    private List<JavaTagBase> fastTags;

    public void register(RythmEngine engine) {
        for (JavaTagBase tag : fastTags) {
            engine.registerFastTag(tag);
        }
    }

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

            o = parameterList.getByName("fullUrl");
            if (null != o) {
                fullUrl = (Boolean) o;
            }

            ActionContext context = ActionContext.current();
            Router router = context.router();

            if (value.contains("/") && fullUrl) {
                int n = parameterList.size();
                Object[] args = new Object[n - 1];
                for (int i = 0; i < n - 1; ++i) {
                    args[i] = parameterList.getByPosition(i + 1);
                }
                p(router._fullUrl(value, args));
            }

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
