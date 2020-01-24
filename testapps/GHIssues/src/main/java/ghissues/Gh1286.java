package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

/**
 * Run app and check if there are exception stack during API doc generation
 */
@UrlContext("1286")
public class Gh1286 extends BaseController {
    public static class TestResp<T> {
        public Integer code;
        public T result;

        public TestResp(Integer code, T result) {
            this.code = code;
            this.result = result;
        }
    }

    public TestResp resp = new TestResp(1, "Hello");

    @GetAction
    public TestResp test() {
        return resp;
    }

}
