package ru.practicum.explorewithme.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.practicum.explorewithme.utils.validation.annotation.DateTimeTwoHoursLater;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeTwoHoursLaterValidator implements ConstraintValidator<DateTimeTwoHoursLater, String> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Clock clock;

    public DateTimeTwoHoursLaterValidator() {
        this.clock = Clock.systemUTC();
    }

    public DateTimeTwoHoursLaterValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(String dateTimeString, ConstraintValidatorContext context) {
        if (dateTimeString == null) {
            return true;
        }
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, formatter);
        return !dateTime.isBefore(LocalDateTime.now(clock).plusHours(2));
    }
}
