package act.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NotBlankHandler implements ConstraintValidator<NotBlank, CharSequence> {

    @Override
    public void initialize(NotBlank constraintAnnotation) {
    }


    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return isValid(value);
    }

    private boolean isValid(Object object) {
        return null != object && object.toString().trim().length() > 0;
    }

}
