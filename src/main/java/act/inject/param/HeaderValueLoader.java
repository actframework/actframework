package act.inject.param;

import act.app.ActionContext;
import act.app.App;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class HeaderValueLoader implements ParamValueLoader {

    private final String key;
    private final boolean multiValues;
    private final boolean isArray;
    private final Class elementType;
    private final Class targetType;
    private final StringValueResolver stringValueResolver;
    private final Object defVal;

    public HeaderValueLoader(String name, BeanSpec beanSpec) {
        this.key = key(name, beanSpec);
        this.targetType = beanSpec.rawType();
        this.isArray = targetType.isArray();
        this.multiValues = (isArray || Collection.class.isAssignableFrom(targetType));
        if (this.isArray) {
            this.elementType = this.targetType.getComponentType();
        } else if (this.multiValues) {
            this.elementType = (Class)(beanSpec.typeParams().get(0));
        } else {
            this.elementType = null;
        }
        Class effectiveType = null != elementType ? elementType : targetType;
        this.stringValueResolver = App.instance().resolverManager().resolver(effectiveType);
        E.illegalArgumentIf(null == this.stringValueResolver, "Cannot find out StringValueResolver for %s", beanSpec);
        this.defVal = StringValueResolverValueLoaderBase.defVal(null, effectiveType);
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (context instanceof ActionContext) {
            return load((ActionContext) context, noDefaultValue);
        }
        throw E.unsupport();
    }

    private Object load(ActionContext context, boolean noDefaultValue) {
        if (multiValues) {
            Iterable<String> iterable = context.req().headers(key);
            List list = new ArrayList();
            if (null != iterable) {
                for (String s : iterable) {
                    Object obj = stringValueResolver.resolve(s);
                    if (null != obj) {
                        list.add(obj);
                    }
                }
            }
            if (isArray) {
                return ArrayLoader.listToArray(list, elementType);
            } else {
                Collection c = (Collection) App.instance().injector().get(targetType);
                c.addAll(list);
                return c;
            }
        } else {
            String value = context.req().header(key);
            Object obj = (null == value) ? null : stringValueResolver.resolve(value);
            return (null == obj) && !noDefaultValue ? defVal : obj;
        }
    }

    private String key(String name,  BeanSpec spec) {
        if (S.notBlank(name)) {
            return name;
        }
        name = spec.name();
        return Keyword.of(name).header();
    }
}
