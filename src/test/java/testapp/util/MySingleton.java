package testapp.util;

import act.app.App;
import act.util.SingletonBase;
import org.osgl.util.E;

import javax.inject.Singleton;

@Singleton
public class MySingleton extends SingletonBase {
    public static MySingleton instance() {
        return App.instance().singleton(MySingleton.class);
    }

    public static <T> T instance2() {
        return (T)App.instance().singleton(MySingleton.class);
    }
}
