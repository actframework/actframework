package act.sys.meta;

import act.app.App;

public enum InvokeType {
    STATIC () {
        @Override
        public <T> T newInstance(Class<T> cls, App app) {
            return null;
        }
    }, VIRTUAL () {
        @Override
        public <T> T newInstance(Class<T> cls, App app) {
            return app.newInstance(cls);
        }
    };
    public abstract <T> T newInstance(Class<T> cls, App app);
}
