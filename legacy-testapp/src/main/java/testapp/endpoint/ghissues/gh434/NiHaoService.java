package testapp.endpoint.ghissues.gh434;

import org.osgl.util.S;

public class NiHaoService implements GreetingService {

    @Override
    public String name() {
        return "你好";
    }

    @Override
    public String greet(String who) {
        return S.concat(who, " 你好");
    }
}
