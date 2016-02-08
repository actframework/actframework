package act.app.data;

import act.ActComponent;
import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import act.controller.meta.ParamMetaInfo;
import act.data.FileBinder;
import act.data.SObjectBinder;
import org.osgl.$;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.mvc.util.StringValueResolver;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.C;
import org.osgl.util.FastStr;
import org.osgl.util.Str;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;

@ActComponent
public class BinderManager extends AppServiceBase<BinderManager> {

    private Map<Object, Binder> binders = C.newMap();

    public BinderManager(App app) {
        super(app);
        registerPredefinedBinders();
        registerBuiltInBinders(app.config());
    }

    @Override
    protected void releaseResources() {
        binders.clear();
    }

    public <T> BinderManager register(Class<T> targetType, Binder<T> binder) {
        binders.put(targetType, binder);
        return this;
    }

    public BinderManager register(ParamMetaInfo paramMetaInfo, Binder binder) {
        binders.put(paramMetaInfo, binder);
        return this;
    }

    public Binder binder(Class<?> clazz, Class<?> componentType, ParamMetaInfo paramMetaInfo) {
        Binder b = binders.get(clazz);
        if (null != b) {
            return b;
        }
        if (clazz.isArray()) {
            Class<?> elementClass = clazz.getComponentType();
            if (Enum.class.isAssignableFrom(elementClass)) {
                return new EnumArrayBinder<>(elementClass);
            }
        }
        if (Collection.class.isAssignableFrom(clazz) && support(componentType)) {
            return new CollectionBinder(componentType, clazz, paramMetaInfo);
        }
        return null;
    }

    public Binder binder(ParamMetaInfo paramMetaInfo) {
        return binders.get(paramMetaInfo);
    }

    private void registerPredefinedBinders() {
        binders.putAll(Binder.predefined());
    }

    private void registerBuiltInBinders(AppConfig config) {
        binders.put(File.class, new FileBinder());
        binders.put(ISObject.class, new SObjectBinder());
        binders.put(SObject.class, new SObjectBinder());
    }

    private static final Set<?> supportedComponentTypes = C.setOf(
            String.class, Boolean.class, Byte.class, Character.class,
            Short.class, Integer.class, Float.class, Long.class, Double.class,
            Str.class, FastStr.class
    );

    private static boolean support(Class<?> type) {
        if (supportedComponentTypes.contains(type)) {
            return true;
        }
        return (Enum.class.isAssignableFrom(type));
    }

    private static class EnumArrayBinder<T> extends Binder<T> {

        private Class<?> enumClass;

        EnumArrayBinder(Class<?> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public T resolve(String model, ParamValueProvider params) {
            String[] sa = params.paramVals(model);
            if (null == sa) {
                return (T)Array.newInstance(enumClass, 0);
            }
            int len = sa.length;
            T t = (T) Array.newInstance(enumClass, len);
            for (int i = 0; i < len; ++i) {
                String s = sa[i];
                Array.set(t, i, Enum.valueOf(((Class<Enum>) enumClass), s));
            }
            return t;
        }

    }

    private class CollectionBinder<T> extends Binder<T> {
        private Class<?> componentType;
        private Class<?> containerType;
        private ParamMetaInfo metaInfo;

        CollectionBinder(Class<?> componentType, Class<?> containerType, ParamMetaInfo metaInfo) {
            this.componentType = componentType;
            this.containerType = containerType;
            this.metaInfo = metaInfo;
        }

        @Override
        public T resolve(String model, ParamValueProvider params) {
            String[] sa = params.paramVals(model);
            if (null == sa) {
                return (T)Array.newInstance(componentType, 0);
            }
            int len = sa.length;
            Collection retVal = container();
            if (null == retVal) {
                return null;
            }
            StringValueResolverManager resolverManager = app().resolverManager();
            for (int i = 0; i < len; ++i) {
                String s = sa[i];
                retVal.add(resolve(s, resolverManager));
            }
            return (T)retVal;
        }

        private Collection container() {
            int modifiers = containerType.getModifiers();
            Collection<?> col = null;
            if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
                if (containerType.isAssignableFrom(AbstractList.class)) {
                    return C.newList();
                } else if (containerType.isAssignableFrom(AbstractSet.class)) {
                    return C.newSet();
                } else {
                    logger.warn("Container type not supported: %s", containerType);
                    return null;
                }
            } else {
                try {
                    return (Collection) $.newInstance(containerType);
                } catch (Exception e) {
                    try {
                        return (Collection) $.newInstance(containerType, 100);
                    } catch (Exception e2) {
                        logger.warn("Cannot initialize container with type: %s", containerType);
                        return null;
                    }
                }
            }
        }

        private Object resolve(String reqVal, StringValueResolverManager resolverManager) {
            StringValueResolver resolver = null;
            if (metaInfo.resolverDefined()) {
                resolver = metaInfo.resolver(app());
            }
            if (null == reqVal) {
                Object ret = metaInfo.defVal(componentType);
                if (null != ret) {
                    return ret;
                }
            }
            if (null == resolver) {
                return resolverManager.resolve(reqVal, componentType);
            } else {
                return resolver.resolve(reqVal);
            }
        }
    }
}
