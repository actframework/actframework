package ghissues;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.handler.ValidateViolationAdvice;
import act.handler.ValidateViolationAdvisor;
import com.alibaba.fastjson.JSONObject;
import org.osgl.aaa.NoAuthentication;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.RenderJSON;
import org.osgl.util.C;

import java.util.Map;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

@UrlContext("922")
@NoAuthentication
public class Gh922 {

    public static class GenkoAdvice implements ValidateViolationAdvice {
        @Override
        public Object onValidateViolation(Map<String, ConstraintViolation> violations, ActionContext context) {
            throw new RenderJSON(violations);
        }
    }

    public static class GlobalAdvice implements ValidateViolationAdvice {
        @Override
        public Object onValidateViolation(Map<String, ConstraintViolation> violations, ActionContext context) {
            JSONObject json = new JSONObject();
            json.put("code", 1);
            json.put("msg", "xxx");
            json.put("count", 180);
            json.put("data", C.Map("item", new int[]{1, 2, 3}));
            return json;
        }
    }


    @GetAction("global")
    public void global(@NotNull String x, @Max(100) int n) {
    }

    @GetAction("specific")
    @ValidateViolationAdvisor(GenkoAdvice.class)
    public void specific(@NotNull String x, @Max(100) int n) {
    }

}
