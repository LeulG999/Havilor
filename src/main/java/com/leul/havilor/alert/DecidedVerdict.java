package com.leul.havilor.alert;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// A custom Bean Validation constraint: the verdict must be an actual decision, not UNDECIDED.
// null is allowed here (combine with @NotNull), the usual convention for constraints.
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DecidedVerdict.Validator.class)
public @interface DecidedVerdict {

    String message() default "must be a decision (MALICIOUS, BENIGN or FALSE_POSITIVE), not UNDECIDED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<DecidedVerdict, Verdict> {

        @Override
        public boolean isValid(Verdict verdict, ConstraintValidatorContext context) {
            return verdict != Verdict.UNDECIDED;
        }
    }
}
