package playground;

import act.app.App;
import act.util.Async;

import java.util.Map;

public class AsyncClass {

    @Async
    public void doIt(final String a, final Map<String, Object> b, int i) {
        App.instance().eventBus().trigger("playground.AsyncClass.doIt_async", a, b, i);
    }

    private void doIt_async(String a, Map<String, Object> b, int i) {
        if (b.containsKey(a)) {
            b.remove(a);
        } else {
            b.put(a, i);
        }
    }

}
