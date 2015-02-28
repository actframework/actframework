package playground;

import org.osgl.http.H;
import org.osgl.mvc.util.Binder;
import org.osgl.util.S;

import java.util.Map;

public class EmailBinder extends Binder<String> {
    @Override
    public String resolve(String argName, H.Request request) {
        String username = request.paramVal("username");
        String host = request.paramVal("host");
        return S.builder(username).append("@").append(host).toString();
    }
}
