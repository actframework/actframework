package ghissues.gh1069;

import java.lang.annotation.*;
import javax.validation.*;

@ReportAsSingleViolation
@Constraint(validatedBy = UniqueValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Unique {

    String message() default "{org.hibernate.validator.constraints.Length.message}";

    Class<?> entity();

    String field();

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}