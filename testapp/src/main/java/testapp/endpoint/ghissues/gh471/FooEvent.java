package testapp.endpoint.ghissues.gh471;

import act.event.ActEvent;

public class FooEvent extends ActEvent<String> {
    public FooEvent(String source) {
        super(source);
    }
}
