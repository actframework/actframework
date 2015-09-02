package act.app;

import act.ActComponent;
import act.util.ClassInfoRepository;
import org.osgl.util.E;

@ActComponent
public class AppClassInfoRepository extends ClassInfoRepository implements AppService {

    private App app;

    public AppClassInfoRepository(App app, ClassInfoRepository actRepository) {
        E.NPE(app);
        this.app = app;
        classes.putAll(actRepository.classes());
    }

    @Override
    public AppHolder app(App app) {
        throw E.unsupport();
    }

    @Override
    public App app() {
        return app;
    }
}
