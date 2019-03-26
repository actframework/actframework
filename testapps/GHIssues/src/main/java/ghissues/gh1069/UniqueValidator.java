package ghissues.gh1069;

import act.app.App;
import act.db.Dao;
import org.apache.bval.jsr.ConstraintValidatorContextImpl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UniqueValidator implements ConstraintValidator<Unique, Object> {

    private Dao dao;
    private String field;

    @Override
    public void initialize(Unique parameters) {
        Class<?> cls = parameters.entity();
        dao = App.instance().dbServiceManager().dao(cls);
        field = parameters.field();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (dao != null && context instanceof ConstraintValidatorContextImpl) {
            return dao.countBy(field, value) <= 0;
        }
        return false;
    }
}