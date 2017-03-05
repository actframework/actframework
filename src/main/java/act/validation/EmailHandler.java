package act.validation;

import org.osgl.util.S;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EmailHandler implements ConstraintValidator<Email, CharSequence> {

    @Override
    public void initialize(Email email) {
    }

    @Override
    public boolean isValid(CharSequence charSequence, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(charSequence);
    }

    private boolean isValid(Object val) {
        String s = S.string(val);
        return (S.isBlank(s) || s.toLowerCase().matches("^[_a-z0-9-']+(\\.[_a-z0-9-']+)*(\\+[0-9]+)?@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})$"));
    }
}
