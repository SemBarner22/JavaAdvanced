package ru.ifmo.rain.zagretdinov.walk;

class RecursiveWalkException extends Throwable {
    RecursiveWalkException(String error_with_args) {
        super(error_with_args);
    }
}
