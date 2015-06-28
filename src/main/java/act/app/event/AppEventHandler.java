package act.app.event;

public interface AppEventHandler {
    String id();
    void onEvent();
}
