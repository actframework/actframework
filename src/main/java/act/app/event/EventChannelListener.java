package act.app.event;

public interface EventChannelListener {
    String id();
    void onEvent();
}
