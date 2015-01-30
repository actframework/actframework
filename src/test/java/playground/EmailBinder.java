package playground;

import org.osgl.mvc.util.Binder;
import org.osgl.util.S;

import java.util.Map;

public class EmailBinder extends Binder<String> {
    @Override
    public String resolve(String argName, Map<String, String[]> params) {
        String username = params.get("username")[0];
        String host = params.get("host")[0];
        return S.builder(username).append("@").append(host).toString();
    }
}
