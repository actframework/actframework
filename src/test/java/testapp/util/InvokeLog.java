package testapp.util;

public interface InvokeLog {

    void invoke(String className, String methodName);

    void invokeStatic(String className, String methodName);
}
