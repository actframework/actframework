package act.app;

import java.io.File;

public class TestingAppClassLoader extends AppClassLoader {
    public TestingAppClassLoader(App app) {
        super(app);
    }

    @Override
    public void preloadClassFile(File base, File file) {
        super.preloadClassFile(base, file);
    }

    @Override
    public void scan2() {
        super.scan2();
    }
}
