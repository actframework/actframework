package act.util;

import act.app.App;
import org.osgl.util.ValueObject;

public class ValueObjectCodecFinder extends SubTypeFinder2<ValueObject.Codec> {
    public ValueObjectCodecFinder() {
        super(ValueObject.Codec.class);
    }

    @Override
    protected void found(Class<ValueObject.Codec> target, App app) {
        ValueObject.register(app.newInstance(target));
    }
}
