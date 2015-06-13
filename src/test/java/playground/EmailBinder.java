package playground;

import org.osgl.http.H;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.S;

import java.util.Map;

public class EmailBinder extends Binder<String> {
    @Override
    public String resolve(String model, ParamValueProvider params) {
        String username = params.paramVal("username");
        String host = params.paramVal("host");
        return S.builder(username).append("@").append(host).toString();
    }
}
