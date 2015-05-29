package act.app;

public abstract class AppServiceBase<T extends AppServiceBase> extends AppHolderBase<T> implements AppService<T> {

    protected AppServiceBase() {
        super();
    }

    protected AppServiceBase(App app) {
        super(app);
        app.register(this);
    }

    @Override
    public T app(App app) {
        app.register(this);
        return super.app(app);
    }

    protected abstract void releaseResources();
}
