package act.data;

import act.app.ActionContext;
import act.app.App;
import act.app.data.StringValueResolverManager;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.C;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AutoBinder {

    private static Logger logger = LogManager.get(AutoBinder.class);

    private CacheService cacheService;
    private StringValueResolverManager resolverManager;

    public AutoBinder(App app) {
        this.cacheService = app.cache();
        this.resolverManager = app.resolverManager();
    }

    public Object resolveSimpleTypeContainer(Collection container, String bindName, ParamValueProvider paramValueProvider, Class<?> componentType) {
        ActionContext ctx = $.cast(paramValueProvider);
        $.T2<Set<String>, Integer> t2 = prepare(bindName, paramValueProvider);
        if (null == t2) {
            return null;
        }
        Set<String> relevantParams = t2._1;
        int prefixLen = t2._2;
        for (String s : relevantParams) {
            String v = ctx.paramVal(s);
            if (S.blank(v)) {
                continue;
            }
            String propertyPath = s.substring(prefixLen);
            if (propertyPath.contains("]")) {
                propertyPath = propertyPath.replace('[', '.').replace("]", "");
            }

            Object o = resolverManager.resolve(v, componentType);
            if (S.blank(propertyPath)) {
                container.add(o);
            } else if (NumberUtils.isDigits(propertyPath)) {
                if (container instanceof List) {
                    List list = (List)container;
                    int index = Integer.parseInt(propertyPath);
                    while (list.size() < index + 1) {
                        list.add(null);
                    }
                    ((List) container).set(Integer.parseInt(propertyPath), o);
                } else {
                    container.add(o);
                }
            } else {
                return null;
            }
        }
        return container;
    }

    public Object resolve(Object entity, String bindName, ParamValueProvider paramValueProvider) {
        ActionContext ctx = $.cast(paramValueProvider);
        $.T2<Set<String>, Integer> t2 = prepare(bindName, paramValueProvider);
        if (null == t2) {
            return null;
        }
        Set<String> relevantParams = t2._1;
        int prefixLen = t2._2;
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

    private $.T2<Set<String>, Integer> prepare(String bindName, ParamValueProvider paramValueProvider) {
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
        return $.T2(relevantParams, prefixLen);
    }
}
