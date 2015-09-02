package act.app;

import act.ActComponent;

import java.io.File;

@ActComponent
public class TestingAppClassLoader extends AppClassLoader {
    public TestingAppClassLoader(App app) {
        super(app);
    }

    @Override
    public void preloadClassFile(File base, File file) {
        super.preloadClassFile(base, file);
    }

    @Override
    public void scan() {
        super.scan();
    }
}
