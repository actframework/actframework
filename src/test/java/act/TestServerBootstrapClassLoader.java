package act;

import act.boot.server.ServerBootstrapClassLoader;
import act.util.ClassNames;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

public class TestServerBootstrapClassLoader extends ServerBootstrapClassLoader {

    public TestServerBootstrapClassLoader(ClassLoader cl) {
        super(cl);
    }

    protected byte[] tryLoadResource(String name) {
        if (!name.startsWith("act.")) return null;
        String fn = ClassNames.classNameToClassFileName(name, true);
        URL url = findResource(fn.substring(1));
        if (null == url) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IO.copy(url.openStream(), baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    @Override
    protected URL findResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }
}
