package ru.practicum.explorewithme.utils.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.practicum.explorewithme.utils.validation.DateTimeTwoHoursLaterValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateTimeTwoHoursLaterValidator.class)
public @interface DateTimeTwoHoursLater {
    String message() default "Event date must be 2 hours after current date minimum";
    Class<?>[] groups() default{};
    Class<? extends Payload>[] payload() default {};
}
