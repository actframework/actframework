package act.test;

import act.util.SingletonBase;
import org.osgl.inject.annotation.MapKey;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class TestEngineManager extends SingletonBase {

    @Inject
    @MapKey("name")
    private Map<String, TestEngine> engineLookup;

    public TestEngine getEngine(String name) {
        TestEngine engine = engineLookup.get(name);
        return null == engine ? engineLookup.get(DefaultTestEngine.NAME) : engine;
    }

}
