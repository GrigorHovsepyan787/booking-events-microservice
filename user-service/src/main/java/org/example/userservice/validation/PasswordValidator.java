package org.example.userservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidator
        implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern UPPERCASE_PATTERN =
            Pattern.compile(".*[A-Z].*");

    private static final Pattern LOWERCASE_PATTERN =
            Pattern.compile(".*[a-z].*");

    private static final Pattern DIGIT_PATTERN =
            Pattern.compile(".*\\d.*");

    private static final Pattern SPECIAL_PATTERN =
            Pattern.compile(".*[@$!%*?&].*");

    private static final int MIN_LENGTH = 8;

    @Override
    public boolean isValid(
            String password,
            ConstraintValidatorContext context
    ) {

        if (password == null) return false;

        boolean hasUpper =
                UPPERCASE_PATTERN.matcher(password).matches();

        boolean hasLower =
                LOWERCASE_PATTERN.matcher(password).matches();

        boolean hasDigit =
                DIGIT_PATTERN.matcher(password).matches();

        boolean hasSpecial =
                SPECIAL_PATTERN.matcher(password).matches();

        boolean hasLength =
                password.length() >= MIN_LENGTH;

        return hasUpper
                && hasLower
                && hasDigit
                && hasSpecial
                && hasLength;
    }
}