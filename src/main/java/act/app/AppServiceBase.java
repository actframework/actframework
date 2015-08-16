package act.app;

public abstract class AppServiceBase<T extends AppServiceBase> extends AppHolderBase<T> implements AppService<T> {

    protected AppServiceBase() {
        super();
    }

    protected AppServiceBase(App app) {
        super(app);
        app.register(this);
    }

    protected AppServiceBase(App app, boolean noDiBinder) {
        super(app);
        app.register(this, noDiBinder);
    }

    @Override
    public T app(App app) {
        app.register(this);
        return super.app(app);
    }

    protected abstract void releaseResources();
}
