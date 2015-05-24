package act.util;

/**
 * Listen to FS events
 */
public interface FsEventListener {
    void on(FsEvent... events);
}
