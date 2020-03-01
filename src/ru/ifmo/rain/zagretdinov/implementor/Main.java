package ru.ifmo.rain.zagretdinov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws ImplerException {
        Implementor implementor = new Implementor();
        implementor.implement(TopKek.class, Path.of("/home/sem/Documents/JavaAdvanced/src/"));
    }
}
