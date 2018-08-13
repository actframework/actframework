package testapp.endpoint.ghissues.gh434;

import org.osgl.util.S;

public class HelloService implements GreetingService {
    @Override
    public String name() {
        return "hello";
    }

    @Override
    public String greet(String who) {
        return S.concat("Hello ", who);
    }
}
