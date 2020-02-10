package ru.ifmo.rain.zagretdinov.walk;

class ArgumentsException extends Exception {
    ArgumentsException(String error_with_args) {
        super("Arguments exception: " + error_with_args);
    }
}
