package ru.ifmo.rain.zagretdinov.bank;

public class ValidateException extends Exception {
    ValidateException() {
        super("Name and surname not the same as in database");
    }
}
