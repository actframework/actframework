package testapp.util;

public class InvokeLogFactory {
    private static InvokeLog log;
    public static void set(InvokeLog log) {
        log = log;
    }
    public static InvokeLog get() {
        return log;
    }
}
