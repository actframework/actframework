package testapp.util;

public class Trackable {

    protected static InvokeLog log() {
        return InvokeLogFactory.get();
    }

    protected void track(String method) {
        log().invoke(className(), method);
    }

    protected static void trackStatic(String className, String method) {
        log().invokeStatic(className, method);
    }

    protected String className() {
        return getClass().getName();
    }
}
