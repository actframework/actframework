package playground;

import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.S;

public class EmailBinder extends Binder<String> {
    @Override
    public String resolve(String bean, String model, ParamValueProvider params) {
        String username = params.paramVal("username");
        String host = params.paramVal("host");
        return S.builder(username).append("@").append(host).toString();
    }
}
