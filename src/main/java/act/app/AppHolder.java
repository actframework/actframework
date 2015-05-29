package act.app;

public interface AppHolder<T extends AppHolder> {
    T app(App app);
    App app();
}
