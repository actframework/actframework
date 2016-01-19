package act.app.conf;

import act.ActComponent;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.event.AppEventId;
import act.event.AppEventListenerBase;
import act.util.AnnotatedTypeFinder;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.E;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EventObject;
import java.util.Map;
import java.util.Set;

@ActComponent
public class AutoConfigPlugin extends AnnotatedTypeFinder {

    private static Field modifiersField;

    public AutoConfigPlugin() {
        super(AutoConfig.class, new $.F2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>>() {
            @Override
            public Map<Class<? extends AppByteCodeScanner>, Set<String>> apply(final App app, final String className) throws NotAppliedException, $.Break {
                app.eventBus().bind(AppEventId.APP_CODE_SCANNED, new AppEventListenerBase() {
                    @Override
                    public void on(EventObject event) throws Exception {
                        Class<?> autoConfigClass = $.classForName(className, app.classLoader());
                        new AutoConfigLoader(app, autoConfigClass).load();
                    }
                });
                return null;
            }
        });
    }

    private static void allowChangeFinalField() {
        try {
            modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    private static void resetFinalFieldUpdate() {
        try {
            modifiersField.setAccessible(false);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    /**
     * Used by plugin that to load their {@link AutoConfig auto configure class}
     *
     * @param autoConfigClass the class with {@link AutoConfig} annotation
     * @param app             the application instance
     */
    public static void loadPluginAutoConfig(Class<?> autoConfigClass, App app) {
        new AutoConfigLoader(app, autoConfigClass).load();
    }

    private static class AutoConfigLoader {
        private App app;
        private Class<?> autoConfigClass;
        private String ns;
        private static boolean setModifierField = false;

        AutoConfigLoader(App app, Class<?> autoConfigClass) {
            this.app = app;
            this.autoConfigClass = autoConfigClass;
            this.ns = (autoConfigClass.getAnnotation(AutoConfig.class)).value();
            synchronized (AutoConfigLoader.class) {
                if (!setModifierField) {
                    allowChangeFinalField();
                    app.jobManager().on(AppEventId.START, new Runnable() {
                        @Override
                        public void run() {
                            resetFinalFieldUpdate();
                        }
                    });
                    setModifierField = true;
                }
            }
        }

        private boolean turnOffFinal(Field field) {
            field.setAccessible(true);
            if (Modifier.isFinal(field.getModifiers())) {
                try {
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                } catch (Exception e) {
                    throw E.unexpected(e);
                }
                return true;
            }
            return false;
        }

        private void turnOnFinal(Field field) {
            try {
                modifiersField.setInt(field, field.getModifiers() | Modifier.FINAL);
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }

        void load() {
            loadClass(autoConfigClass, ns);
        }

        void loadClass(Class<?> c, String ns) {
            Class[] ca = c.getClasses();
            for (Class c0 : ca) {
                int mod = c0.getModifiers();
                if (Modifier.isStatic(mod)) {
                    loadClass(c0, ns + "." + c0.getSimpleName());
                }
            }
            Field[] fa = c.getDeclaredFields();
            for (Field f : fa) {
                if (Modifier.isStatic(f.getModifiers())) {
                    loadAutoConfig_(f, ns);
                }
            }
        }

        private void loadAutoConfig_(Field f, String ns) {
            String key = ns + "." + f.getName();
            Object val = app.config().getIgnoreCase(key);
            if (null == val) {
                // try to change the "prefix.key_x_an_y" form to "prefix.key.x.an.y" form
                key = key.replace('_', '.');
                val = app.config().getIgnoreCase(key);
                if (null == val) {
                    return;
                }
            }
            Class<?> type = f.getType();
            boolean isFinal = false;
            try {
                // we really want it to be public, so comment out: f.setAccessible(true);
                isFinal = turnOffFinal(f);
                if (String.class.equals(type)) {
                    f.set(null, val);
                } else if (Integer.TYPE.equals(type) || Integer.class.equals(type)) {
                    f.set(null, Integer.parseInt(val.toString()));
                } else if (Boolean.TYPE.equals(type) || Boolean.class.equals(type)) {
                    f.set(null, Boolean.parseBoolean(val.toString()));
                } else if (Character.TYPE.equals(type) || Character.class.equals(type)) {
                    f.set(null, val.toString().charAt(0));
                } else if (Byte.TYPE.equals(type) || Byte.class.equals(type)) {
                    f.set(null, Byte.parseByte(val.toString()));
                } else if (Long.TYPE.equals(type) || Long.class.equals(type)) {
                    f.set(null, Long.parseLong(val.toString()));
                } else if (Float.TYPE.equals(type) || Float.class.equals(type)) {
                    f.set(null, Float.parseFloat(val.toString()));
                } else if (Double.TYPE.equals(type) || Double.class.equals(type)) {
                    f.set(null, Double.parseDouble(val.toString()));
                } else if (Enum.class.isAssignableFrom(type)) {
                    f.set(null, Enum.valueOf(((Class<Enum>) type), val.toString()));
                } else {
                    logger.warn("Config[%s] field type[%s] not recognized", key, type);
                }
            } catch (Exception e) {
                throw E.invalidConfiguration("Error get configuration " + key + ": " + e.getMessage());
            } finally {
                if (isFinal) {
                    turnOnFinal(f);
                }
            }
        }
    }

}
