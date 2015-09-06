package act.validation;

import act.ActComponent;
import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;

import javax.validation.constraints.Digits;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

@ActComponent
public class DigitsHandler extends ActionMethodParamAnnotationHandlerPlugin {

    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Digits.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null == paramVal) return;
        Digits theAnno = (Digits) annotation;
        int integral = theAnno.integer();
        int fraction = theAnno.fraction();
        if (paramVal instanceof CharSequence
                || paramVal instanceof Integer
                || paramVal instanceof Byte
                || paramVal instanceof Short
                || paramVal instanceof Long
                || paramVal instanceof Double
                || paramVal instanceof Float
                || paramVal instanceof BigDecimal
                || paramVal instanceof BigInteger) {
            validate(paramVal.toString(), integral, fraction, theAnno, context);
        }
    }

    private void validate(String val, int integer, int fraction, Digits theAnno, ActionContext context) {
        boolean violated;
        if (fraction > 0) {
            if (val.contains(".")) {
                String[] sa = val.split("\\.");
                if (sa.length == 2) {
                    violated = (sa[0].replaceAll("[\\,\\s]+", "").length() != integer || sa[1].length() != fraction);
                } else {
                    violated = true;
                }
            } else {
                violated = true;
            }
        } else {
            if (val.contains(".")) {
                violated = true;
            } else {
                violated = val.replaceAll("[\\s\\,]+", "").length() != integer;
            }
        }
        if (violated) {
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(val, theAnno.message(), theAnno, context));
        }
    }

}
