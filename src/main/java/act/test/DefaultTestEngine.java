package act.test;

import act.util.ProgressGauge;
import act.util.SingletonBase;

import javax.validation.ValidationException;

public class DefaultTestEngine extends SingletonBase implements TestEngine {

    public static final String NAME = "default";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void validate(Scenario scenario, TestSession session) throws ValidationException {
        scenario.validate(session);
    }

    @Override
    public boolean run(Scenario scenario, TestSession session, ProgressGauge gauge) {
        return scenario.runInteractions(session, gauge);
    }
}
