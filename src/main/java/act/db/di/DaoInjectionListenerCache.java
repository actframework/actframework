package act.db.di;

import act.app.App;
import act.app.AppServiceBase;
import org.osgl.$;
import org.osgl.util.C;

import java.util.Map;

public class DaoInjectionListenerCache extends AppServiceBase<DaoInjectionListenerCache> {

    private Map<$.T2<Class, Class>, DaoInjectionListener> cache = C.newMap();

    public DaoInjectionListenerCache(App app) {
        super(app);
    }

    @Override
    protected void releaseResources() {
        cache.clear();
    }

    public DaoInjectionListener get($.T2<Class, Class> key) {
        return cache.get(key);
    }

    public void put($.T2<Class, Class> key, DaoInjectionListener listener) {
        cache.put(key, listener);
    }


}
