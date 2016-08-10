package act.app.data;

import act.ActComponent;
import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import act.controller.meta.HandlerParamMetaInfo;
import act.data.FileBinder;
import act.data.SObjectBinder;
import org.osgl.$;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.C;
import org.osgl.util.FastStr;
import org.osgl.util.Str;
import org.osgl.util.StringValueResolver;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;

import static act.app.App.logger;

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

    public BinderManager register(HandlerParamMetaInfo paramMetaInfo, Binder binder) {
        binders.put(paramMetaInfo, binder);
        return this;
    }

    public Binder binder(Class<?> clazz, Class<?> componentType, HandlerParamMetaInfo paramMetaInfo) {
        return binders.get(clazz);
    }

    public Binder binder(HandlerParamMetaInfo paramMetaInfo) {
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
}
