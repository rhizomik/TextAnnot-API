package cat.udl.eps.entsoftarch.textannot.domain.validator;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TagConstraintValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TagConstraint {

    String message() default "Invalid tag hierarchy";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
