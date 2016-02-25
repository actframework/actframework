package act.data;

import act.app.ActionContext;
import act.app.App;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Set;

public class AutoBinder {

    private static Logger logger = LogManager.get(AutoBinder.class);

    private CacheService cacheService;

    public AutoBinder(App app) {
        this.cacheService = app.cache();
    }

    public Object resolve(Object entity, String bindName, ParamValueProvider paramValueProvider) {
        ActionContext ctx = $.cast(paramValueProvider);
        Set<String> params = ctx.allParams().keySet();
        Set<String> relevantParams = C.newSet();
        String prefix = bindName;
        for (String s : params) {
            String p2;
            if (s.contains(".")) {
                p2 = prefix + ".";
            } else if (s.contains("[")) {
                p2 = prefix + "[";
            } else {
                continue;
            }
            if (s.startsWith(p2)) {
                relevantParams.add(s);
            }
        }
        if (relevantParams.isEmpty()) {
            return null;
        }
        int prefixLen = prefix.length() + 1;
        for (String s : relevantParams) {
            String v = ctx.paramVal(s);
            if (S.blank(v)) {
                continue;
            }
            String propertyPath = s.substring(prefixLen);
            try {
                $.setProperty(cacheService, entity, v, propertyPath);
            } catch (RuntimeException e) {
                Throwable t = e.getCause();
                if (t instanceof NoSuchFieldException) {
                    logger.warn("Cannot assign to unknown property %s on class %s", propertyPath, entity.getClass());
                    // ignore now
                    // TODO: cache this information so that next time we won't trigger it again
                } else {
                    throw e;
                }
            }
        }
        return entity;
    }
}
