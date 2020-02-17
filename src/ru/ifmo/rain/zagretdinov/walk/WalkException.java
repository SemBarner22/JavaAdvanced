package ru.ifmo.rain.zagretdinov.walk;


class WalkException extends Exception {
    WalkException(String message) {
        super(message);
    }
    WalkException(String message, Throwable e) {
        super(message, e);
    }
}
