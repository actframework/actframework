package testapp.endpoint.ghissues.gh434;

import org.osgl.util.S;

public class ZaiJianService implements FarewellService {
    @Override
    public String bye(String who) {
        return S.concat(who, " 再见");
    }

    @Override
    public String name() {
        return "再见";
    }
}
