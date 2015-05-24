package act.boot.server;

import java.lang.reflect.Method;

/**
 * bootstrap the Act server instance
 */
public class RunServer {
    public static void main(String[] args) throws Exception {
        ServerBootstrapClassLoader classLoader = new ServerBootstrapClassLoader(RunServer.class.getClassLoader());
        Class<?> actClass = classLoader.loadClass("Act");
        Method m = actClass.getDeclaredMethod("startServer");
        m.invoke(null);
    }
}
