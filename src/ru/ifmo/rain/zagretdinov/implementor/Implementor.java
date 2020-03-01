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

public class Implementor implements Impler {
    private BufferedWriter bufferedWriter;
    private String wordsSeparator = " ";
    private String operatorSeparator = ";";
    private String beginningOfBlock = "{";
    private String endOfBlock = "}";
    private String openBracket = "(";
    private String closeBracket = ")";

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
        String classOrInterface = "class";
        String extendsOrImplements = token.isInterface() ? "implements" : "extends";
        try {
            bufferedWriter = Files.newBufferedWriter(Path.of(place.toString(), getClassName(token) + ".java"));
            bufferedWriter.write("package " + token.getPackageName() + operatorSeparator + System.lineSeparator());
            bufferedWriter.flush();
            bufferedWriter.write(getClassModifiers(token) + " " + classOrInterface
                    + " " + token.getSimpleName() + "Impl " + extendsOrImplements + " " + token.getCanonicalName() + " "
                    + beginningOfBlock + System.lineSeparator());
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new ImplerException("Wrong path");
        }
        allWork(token, root);
        try {
            bufferedWriter.write(endOfBlock + System.lineSeparator());
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new ImplerException("Wrong path");
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

    private void allWork(Class<?> token, Path root) {
        Arrays.stream(token.getDeclaredConstructors()).forEach(constructor -> {
            try {
                String[] strings = getParameters(constructor);
                if (strings[0] == null) {
                    strings[0] = "";
                }
                if (strings[1] == null) {
                    strings[1] = "";
                }
                bufferedWriter.write(token.getSimpleName() + "Impl" + openBracket);
                bufferedWriter.write(strings[0] + closeBracket + beginningOfBlock + System.lineSeparator());
                bufferedWriter.write('\t' + "super" + openBracket + strings[1] + closeBracket + operatorSeparator + System.lineSeparator() + endOfBlock + System.lineSeparator());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Set<Integer> methodsHashed = new HashSet<>();
        Arrays.stream(token.getMethods()).forEach(method -> {
                StringBuilder hashing = new StringBuilder();
                hashing.append(method.getReturnType().toString());
                for (Class<?> m : method.getParameterTypes()) {
                    hashing.append(m.getCanonicalName());
                }
                int hash = hashing.hashCode();
                if (methodsHashed.add(hash)) {
                    if ((method.getModifiers() & Modifier.STATIC) == 0
                            && (method.getModifiers() & Modifier.ABSTRACT) != 0) {
                    methodWalk(method);
                }
            }
        });

        Class cur = token;
        while (cur != null) {
            Arrays.stream(cur.getDeclaredMethods()).forEach(method -> {
                    StringBuilder hashing = new StringBuilder();
                    hashing.append(method.getReturnType().toString());
                    for (Class<?> m : method.getParameterTypes()) {
                        hashing.append(m.getCanonicalName());
                    }
                    int hash = hashing.hashCode();
                    if (methodsHashed.add(hash)) {
                        if ((method.getModifiers() & Modifier.ABSTRACT) != 0
                                && (method.getModifiers() & Modifier.PROTECTED) != 0) {
                        methodWalk(method);
                    }
                }
            });
            cur = cur.getSuperclass();
        }
    }

    private void methodWalk(Method method) {
        try {
            String result = getDefaultValue(method.getReturnType());
            bufferedWriter.write("@Override" + System.lineSeparator());
            bufferedWriter.write(getMethodModifiers(method) + " " + method.getReturnType().getCanonicalName() +
                    " " + method.getName() + " " + openBracket + getParameters(method) + closeBracket + " " +
                    getThrowable(method) + beginningOfBlock + System.lineSeparator());
            bufferedWriter.write('\t' + "return " + result + operatorSeparator + System.lineSeparator() + endOfBlock);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getThrowable(Method method) {
        StringBuilder result = new StringBuilder();
        if (method.getExceptionTypes().length == 0) {
            return "";
        } else {
            result.append("throws").append(" ");
        }
        for (Class t : method.getExceptionTypes()) {
            result.append(t.getName()).append(",").append(" ");
        }
        result.delete(result.length() - 2, result.length());
        return result.toString();
    }

    private String getParameters(Method method) {
        StringBuilder result = new StringBuilder();
        if (method.getParameterTypes().length == 0) {
            return "";
        }
        int number = 1;
        for (Class clazz : method.getParameterTypes()) {
            result.append(clazz.getCanonicalName()).append(" ").append("_" + number).append(",").append(" ");
            number++;
        }
        result.delete(result.length() - 2, result.length());
        return result.toString();
    }

    private String[] getParameters(Constructor constructor) {
        StringBuilder[] result = new StringBuilder[2];
        result[0] = new StringBuilder();
        result[1] = new StringBuilder();
        if (constructor.getParameterTypes().length == 0) {
            String[] strings = new String[2];
            return strings;
        }
        int number = 1;
        for (Class clazz : constructor.getParameterTypes()) {
            result[0].append(clazz.getCanonicalName()).append(" ").append("_" + number).append(",").append(" ");
            result[1].append("_" + number).append(",").append(" ");
            number++;
        }
        result[0].delete(result[0].length() - 2, result[0].length());
        result[1].delete(result[1].length() - 2, result[1].length());
        String[] strings = new String[2];
        strings[0] = result[0].toString();
        strings[1] = result[1].toString();
        return strings;
    }

    private String getClassModifiers(Class<?> token) {
        return Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.INTERFACE);
    }

    private String getMethodModifiers(Method m) {
        return Modifier.toString(m.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
    }
}

