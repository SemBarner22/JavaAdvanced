package ru.ifmo.rain.zagretdinov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Implementor implements Impler {
    private static final String TAB = "\t";
    private String SPACE = " ";
    private String LINE_SEP = System.lineSeparator();
    private String COLLECTION_SEPARATOR = ", ";
    private String OPER_SEP = ";";
    private String BLOCK_BEGIN = "{";
    private String BLOCK_END = "}";
    private String BRACKET_OPEN = "(";
    private String BRACKET_END = ")";

    private <T> String elementsToString(String delimiter, T[] elements, Function<T, String> transform) {
        return Arrays.stream(elements).map(transform).collect(Collectors.joining(delimiter));
    }

    private <T> String elementsInCollectionToString(T[] items, Function<T, String> transform) {
        return elementsToString(COLLECTION_SEPARATOR, items, transform);
    }

    private String elementsSpaced(String... parts) {
        return Arrays.stream(parts).collect(Collectors.joining(SPACE));
    }

    private String elementsLineSeparated(String... blocks) {
        return Arrays.stream(blocks).collect(Collectors.joining(LINE_SEP + LINE_SEP));
    }

    private String Tabs(int cnt) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cnt; i++) {
            builder.append(TAB);
        }
        return builder.toString();
    }

    private String getIfNotEmpty(String prefix, String itemList) {
        if (!"".equals(itemList)) {
            return elementsSpaced(prefix, itemList);
        }
        return "";
    }

    private String getPackage(Class<?> token) {
        return getIfNotEmpty("package", token.getPackageName()) + OPER_SEP;
    }

    private String getFilePath(Class<?> token) {
        return token.getPackageName().replace('.', File.separatorChar);
    }

    private String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Arguments should not be null");
        }
        Path place;
        try {
            place = Path.of(root.toString(), getFilePath(token));
        } catch (InvalidPathException e) {
            throw new ImplerException("Wrong path");
        }
        try {
            Files.createDirectories(place);
        } catch (IOException e) {
            throw new ImplerException("Wrong path");
        }
        if (token.isPrimitive() || token.isArray() ||
                Modifier.isFinal(token.getModifiers()) || token == Enum.class) {
            throw new ImplerException("Unsupported class token given");
        }
        String extendsOrImplements = token.isInterface() ? "implements" : "extends";
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
                Path.of(place.toString(), getClassName(token) + ".java"))) {
            bufferedWriter.write(elementsLineSeparated(getPackage(token), LINE_SEP));
            bufferedWriter.write(elementsSpaced(getClassModifiers(token),
                    "class", getClassName(token), extendsOrImplements,
                    token.getCanonicalName(), BLOCK_BEGIN, LINE_SEP));
            allWork(token, bufferedWriter);
            bufferedWriter.write(LINE_SEP + BLOCK_END + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDefaultValue(Class<?> ret) {
        if (!ret.isPrimitive()) {
            return "null";
        } else if (ret.equals(void.class)) {
            return "";
        } else if (ret.equals(boolean.class)) {
            return "false";
        } else {
            return "0";
        }
    }

    private void methodWalker(Set<Integer> methodsHashed, Method[] methods, int modifier1, int modifier2,
                              BufferedWriter bufferedWriter) {
        Arrays.stream(methods).forEach(method -> {
            StringBuilder hashing = new StringBuilder();
            hashing.append(method.getReturnType().toString());
            for (Class<?> m : method.getParameterTypes()) {
                hashing.append(m.getCanonicalName());
            }
            int hash = hashing.hashCode();
            if (methodsHashed.add(hash)) {
                if ((method.getModifiers() & modifier1) != 0
                        && (method.getModifiers() & modifier2) != 0) {
                    methodWalk(method, bufferedWriter);
                }
            }
        });
    }

    private class Indices {
        int index = 1;
        Integer add() {
            return index++;
        }
    }

    private void allWork(Class<?> token, BufferedWriter bufferedWriter) throws ImplerException {
        if (!token.isInterface()) {
            List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                    .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                    .collect(Collectors.toList());
            if (constructors.isEmpty()) {
                throw new ImplerException("Class with no non-private constructors can not be extended");
            }
            Arrays.stream(token.getDeclaredConstructors()).forEach(constructor -> {
                try {
                    bufferedWriter.write(getEverything(constructor, token));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        Set<Integer> methodsHashed = new HashSet<>();
        methodWalker(methodsHashed, token.getMethods(), ~Modifier.STATIC, Modifier.ABSTRACT, bufferedWriter);
        Class cur = token;
        while (cur != null) {
            methodWalker(methodsHashed, cur.getDeclaredMethods(), Modifier.ABSTRACT, Modifier.PROTECTED, bufferedWriter);
            cur = cur.getSuperclass();
        }
    }

    private void methodWalk(Method method, BufferedWriter bufferedWriter) {
        try {
            bufferedWriter.write(getEverything(method));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getEverything(Method method) {
        return Tabs(1) + "@Override" + LINE_SEP + Tabs(1) + getMethodModifiers(method) + " " +
                method.getReturnType().getCanonicalName() + " " + method.getName() + " " +
                BRACKET_OPEN + getParameters(method.getParameterTypes()) + BRACKET_END + " "
                + getThrowable(method.getExceptionTypes()) + " " + BLOCK_BEGIN + LINE_SEP
                + Tabs(2) + "return " + getDefaultValue(method.getReturnType()) +
                OPER_SEP + System.lineSeparator() + Tabs(1) + BLOCK_END;
    }

    private String getEverything(Constructor constructor, Class<?> token) {
        return Tabs(1) + getClassName(token) + BRACKET_OPEN + getParameters(constructor.getParameterTypes()) +
                BRACKET_END + " " + getThrowable(constructor.getExceptionTypes()) + BLOCK_BEGIN + LINE_SEP + Tabs(2)
                + "super" + BRACKET_OPEN + getParametersNumbers(constructor.getParameterTypes()) + BRACKET_END + OPER_SEP + LINE_SEP
                + BLOCK_END + LINE_SEP;
    }

    private String getThrowable(Class[] exceptionTypes) {
        return exceptionTypes.length == 0 ? "" : "throws " + elementsToString(COLLECTION_SEPARATOR, exceptionTypes, Class::getName);
    }

    private String getParameters(Class[] parameterTypes) {
        Indices indices = new Indices();
        return parameterTypes.length == 0 ? "" :
                elementsInCollectionToString(parameterTypes, parameter -> elementsSpaced(parameter.getCanonicalName(), "_" + indices.add()));
    }

    private String getParametersNumbers(Class[] parameterTypes) {
        Indices indices = new Indices();
        return parameterTypes.length == 0 ? "" :
                elementsInCollectionToString(parameterTypes, parameter -> elementsSpaced("_" + indices.add()));
    }

    private String getClassModifiers(Class<?> token) {
        return Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.INTERFACE);
    }

    private String getMethodModifiers(Method m) {
        return Modifier.toString(m.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
    }
}

