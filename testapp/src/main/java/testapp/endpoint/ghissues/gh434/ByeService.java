package testapp.endpoint.ghissues.gh434;

import org.osgl.util.S;

public class ByeService implements FarewellService {
    @Override
    public String bye(String who) {
        return S.concat("Bye ", who);
    }

    @Override
    public String name() {
        return "bye";
    }
}
