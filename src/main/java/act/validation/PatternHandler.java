package act.validation;

import act.app.ActionContext;
import act.controller.ActionMethodParamAnnotationHandlerPlugin;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.validation.constraints.Pattern;
import java.lang.annotation.Annotation;
import java.util.Set;

public class PatternHandler extends ActionMethodParamAnnotationHandlerPlugin {
    @Override
    public Set<Class<? extends Annotation>> listenTo() {
        Set<Class<? extends Annotation>> set = C.newSet();
        set.add(Pattern.class);
        return set;
    }

    @Override
    public void handle(String paramName, Object paramVal, Annotation annotation, ActionContext context) {
        if (null == paramVal) {
            return;
        }
        if (!(paramVal instanceof CharSequence)) {
            throw E.unexpected("Invalid param type. expected: CharSequence, found: %s", paramVal.getClass());
        }
        String val = paramVal.toString();
        Pattern theAnno = (Pattern) annotation;
        String pattern = theAnno.regexp();
        Pattern.Flag[] flags = theAnno.flags();
        int flag = 0;
        for (Pattern.Flag f: flags) {
            flag &= f.getValue();
        }
        java.util.regex.Pattern P = java.util.regex.Pattern.compile(pattern, flag);
        if (!P.matcher(val).matches()) {
            context.addViolation(new ActionMethodParamConstraintViolation<Object>(theAnno.message(), theAnno, context));
        }
    }

}
