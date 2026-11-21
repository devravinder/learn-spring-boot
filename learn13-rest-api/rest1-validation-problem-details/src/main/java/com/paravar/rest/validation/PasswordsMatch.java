package com.paravar.rest.validation;

import com.paravar.rest.users.UserRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordsMatch.Validator.class)
public @interface PasswordsMatch {

    String message() default "{password.mismatch}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PasswordsMatch, UserRequest> {
        @Override
        public boolean isValid(UserRequest user, ConstraintValidatorContext context) {
            if (Objects.equals(user.password(), user.confirmPassword())) {
                return true;
            }
            // report on the field, not on the object, so clients can highlight the input
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword").addConstraintViolation();
            return false;
        }
    }
}
