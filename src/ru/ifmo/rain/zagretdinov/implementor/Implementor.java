package ru.ifmo.rain.zagretdinov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class implements {@link Impler}. Provides methods to implement {@code .java} files
 * from given class or interface.
 *
 * @author sem
 * @version 1.0
 */
public class Implementor implements Impler {
    // :NOTE: Константы? //DONE
    /**
     * Tabulation constant for generated class.
     */
    private static final String TAB = "\t";

    /**
     * Space constant for generated class.
     */
    private static final String SPACE = " ";

    /**
     * Line-separator constant for generated class.
     */
    private static final String LINE_SEP = System.lineSeparator();

    /**
     * Separator constant for tokens in generated class.
     */
    private static final String COLLECTION_SEPARATOR = ", ";

    /**
     * Function used to determine whether generate {@code .jar} or {@code .java} file.
     * Continues working with {@link Implementor} in scenarios:
     * 2 arguments {@code className outputPath}: creates {@code .java} file executing
     * method {@link #implement(Class, Path)} provided by interface {@link Impler}
     * Arguments should not be null. If input is incorrect or an error happens during executing
     * message is printed and execution is aborted.
     *
     * @param args console line arguments: {@code [-jar] className outputPath}
     */
    public static void main(final String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Invalid arguments number, expected <class.name> <output.path>");
        } else {
            for (final String arg : args) {
                if (arg == null) {
                    System.err.println("All arguments should be not null");
                    return;
                }
            }
            try {
                new Implementor().implement(Class.forName(args[0]), Path.of(args[1]));
            } catch (final ClassNotFoundException e) {
                System.err.println("Invalid class name given: " + e.getMessage());
            } catch (final InvalidPathException e) {
                System.err.println("Invalid path given: " + e.getMessage());
            } catch (ImplerException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Returns default value for a given class. returns value based on default value of a given {@link Class}
     * @param clazz default value for which should be returned
     * @return {@link String} containing default value for a given {@link Class}
     */
    private String getDefaultValue(final Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return "null";
        } else if (clazz.equals(void.class)) {
            return "";
        } else if (clazz.equals(boolean.class)) {
            return "false";
        } else {
            return "0";
        }
    }

    /**
     * Writes a string with collection of exceptions. Prepends with "throws" and  and appends with {@link #SPACE}
     * if at least one {@link Exception} could be thrown from given {@link Executable}.
     * @param executable given {@link Executable}
     * @return {@link String} containing collection of {@link Exception}s, separated by {@link #COLLECTION_SEPARATOR}
     */
    private String exceptionsOfExecutable(final Executable executable) {
        return emptyOrPrefix("throws", collectionTransformedElements(executable.getExceptionTypes(),
                Class::getCanonicalName), SPACE);
    }

    /**
     * returns enumeration of items separated by {@code System.lineSeparator()} prepended with {@link String}
     * prefix and appended with {@link String} delimiter.
     * @param prefix {@link String} which prepends items
     * @param items {@link String} string which has prefix and postfix
     * @param delimiter {@link String} which appends items
     * @return empty {@link String} if {@code items} are empty, otherwise returns concatenated string.
     */
    private String emptyOrPrefix(final String prefix, final String items, final String delimiter) {
        if (!items.isEmpty()) {
            return elementsSeparated(System.lineSeparator(), prefix, items) + delimiter;
        }
        return "";
    }

    /**
     * Writes class modifiers. Gets {@link Modifier} of {@link Class} and writes a string containing all of them
     * excluding {@code Modifier.ABSTRACT}, {@code Modifier.INTERFACE}, {@code Modifier.STATIC}
     * and {@code Modifier.PROTECTED}
     * @param token {@link Class} modifiers of which are expected in return.
     * @return {@link String} containing modifiers of a given method excluding {@code Modifier.ABSTRACT},
     * {@code Modifier.INTERFACE}, {@code Modifier.STATIC} and {@code Modifier.PROTECTED}
     */
    private String getClassModifiers(final Class<?> token) {
        return Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT &
                ~Modifier.INTERFACE & ~Modifier.STATIC & ~Modifier.PROTECTED);
    }

    /**
     * Concatenates given not-null strings separated by {@code delimiter}. Preliminarily filters only {@link String}s,
     * which are not empty.
     * @param delimiter string used to join {@code parts}
     * @param parts strings to concatenate.
     * @return {@link String} consisting of concatenated given strings joined by given delimiter.
     */
    private String elementsSeparated(final String delimiter, final String... parts) {
        return Arrays.stream(parts).filter(s -> !"".equals(s)).collect(Collectors.joining(delimiter));
    }

    /**
     * Writes to generated class body of a given method. Writes its modifiers using {@link #getMethodModifiers(Method)},
     * its name, parameters using {@link #getParameters(Executable)}, throwable using
     * {@link #exceptionsOfExecutable(Executable)} and a block with {@link #getDefaultValue(Class)}.
     * @param method target method.
     * @return {@link String} consisting of body of a given method.
     */
    private String getMethodBody(final Method method) {
        return elementsSpaced(elementsLineSeparated(tabs(1),
                tabs(1) + getMethodModifiers(method),
                method.getReturnType().getCanonicalName(), method.getName(),
                getParameters(method),
                exceptionsOfExecutable(method),
                "{", tabs(2) + "return",
                getDefaultValue(method.getReturnType()) + ";", tabs(1) + "}"));
    }

    /**
     * Inner class of {@link Implementor} which provides methods to put {@link Method} in a {@link HashSet} or
     * {@link HashMap}. Class needed to filter methods on the hierarchy of a given {@link Class} to {@link Implementor}.
     */
    private class HashableMethod {
        /**
         * Link on a {@link Method} this instance of {@link HashableMethod} is based on.
         */
        private final Method method;

        /**
         * Default constructor. Creates new instance of {@link HashableMethod} out of
         * given {@link Method} class using {@code super()}
         * @param method given {@link Method}
         */
        HashableMethod(final Method method) {
            this.method = method;
        }

        /**
         * Getter for a private field {@code method}.
         * @return field of this instance of {@link HashableMethod}
         */
        Method get() {
            return method;
        }

        /**
         * Hashcode for an instance of {@link HashableMethod}.
         * @return {@link Integer} hashcode for an instance of {@link HashableMethod}.
         */
        @Override
        public int hashCode() {
            final int BASE = 1000000007;
            final int PRIME = 31;
            return ((method.getName().hashCode() +
                    PRIME * Arrays.hashCode(method.getParameterTypes())) % BASE +
                    (PRIME * PRIME) % BASE * method.getReturnType().hashCode()) % BASE;
        }

        /**
         * Checks whether hashcode of field {@code method} is equal to a hashcode of a given {@link Object}
         * @param obj {@link Object} which hashcode is to compare.
         * @return {@code true} if hashcodes are equal and {@code false} otherwise.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof HashableMethod) {
                final HashableMethod hm = (HashableMethod) obj;
                return method.getName().equals(hm.method.getName()) &&
                        Arrays.equals(method.getParameterTypes(), hm.method.getParameterTypes()) &&
                        method.getReturnType().equals(hm.method.getReturnType());
            }
            return false;
        }
    }


    /**
     * return {@link String} of methods of {@code token} but only non-private
     * separated by lineSeparator
     * @param token instance of {@link Class}
     * @return {@link String} of methods of {@code token}
     * @throws ImplerException if a given class is private.
     */
    private String getMethods(Class<?> token) throws ImplerException {
        final Set<HashableMethod> methods = new HashSet<>();
        Arrays.stream(token.getMethods()).map(HashableMethod::new).forEach(methods::add);
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't override private class");
        }
        while (token != null) {
            Arrays.stream(token.getDeclaredMethods()).map(HashableMethod::new).forEach(methods::add);
            token = token.getSuperclass();
        }
        return methods.stream().filter(a -> Modifier.isAbstract(a.get().getModifiers()))
                .map(a -> getMethodBody(a.get())).collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Writes method modifiers. Gets {@link Modifier} of {@link Method} and writes a string containing all of them
     * excluding {@code Modifier.ABSTRACT} and {@code Modifier.TRANSIENT}
     *
     * @param m methods modifiers of which are expected in return.
     * @return {@link String} containing modifiers of a given method excluding {@code Modifier.ABSTRACT} and
     * {@code Modifier.TRANSIENT}
     */
    private String getMethodModifiers(final Method m) {
        return Modifier.toString(m.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
    }

    /**
     * Joins elements with {@link #LINE_SEP}.
     *
     * @param cnt amount of {@link #TAB} to be written.
     * @return a string containing {@link #TAB} given {@link Integer} times.
     */
    // :NOTE: Метод с большой буквы //DONE
    private String tabs(final int cnt) {
        return TAB.repeat(Math.max(0, cnt));
    }

    /**
     * return {@link String} of constructors of {@code token}  but only non-private
     * separated by lineSeparator
     * @param token instance of {@link Class}
     * @return {@link String} of constructors of {@code token}
     * @throws ImplerException if all constructors are private, or there are no constructors.
     */
    private String getConstructors(final Class<?> token) throws ImplerException {
        if (!token.isInterface()) {
            final Optional<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                    .filter(c -> !Modifier.isPrivate(c.getModifiers())).findAny();
            if (constructors.isEmpty()) {
                throw new ImplerException("Class with no non-private constructors is not extendable");
            }
            return constructors.stream()
                    .map(a -> getConstructorBody(a, token))
                    .collect(Collectors.joining(System.lineSeparator()));
        } else {
            return "";
        }
    }


    /**
     * Produces a {@link String} of parameters and/or unique identifiers for them. Creates a {@link String} consisting
     * of separated results of functions to each element with unique names..
     * @param executable method/constructor which parameter types to be given unique identifiers.
     * @param function to get {@link String} from each element
     * @return {@link String} containing collection of results of {@link Function} bounded by brackets
     * to given {@link Array} of elements
     * with unique names to each element.
     */
    private String parameterIdentifiers(final Executable executable, Function<Parameter, String> function) {
        return Arrays.stream(executable.getParameters())
                .map(function)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Produces a {@link String} of method parameters. Uses {@link #parameterIdentifiers(Executable, Function)} )}
     * to get different identifier to the instance of each parameter
     * @param executable method/constructor which parameter types to be given unique identifiers.
     * @return {@link String} containing collection of pairs consisting of parameter type, {{@link #SPACE},}
     * unique identifier, separated by {@link #COLLECTION_SEPARATOR}
     */
    private String getParameters(final Executable executable) {
        return parameterIdentifiers(executable, parameter -> parameter.getType().getCanonicalName()
                + SPACE + parameter.getName());
    }

    /**
     * Produces a {@link String} of method parameters identifiers. Uses
     * {@link #parameterIdentifiers(Executable, Function)}
     * to get different identifier to the instance of each parameter
     * @param executable method/constructor which parameter types of is needed
     * @return {@link String} containing collection of  separated by {@link #SPACE}
     */
    private String getParametersNumbers(final Executable executable) {
        return parameterIdentifiers(executable, Parameter::getName);
    }

    /**
     * Transforms given array of elements to strings and concatenated them with a delimiter.
     * @param elements given elements to be transformed and concatenated.
     * @param transform {@link Function} from type of elements to {@link String}
     * @param <T> type of elements given
     * @return {@link String} containing concatenated transformed to {@link String} array of elements separated by
     * {@link #COLLECTION_SEPARATOR}
     */
    private <T> String collectionTransformedElements(final T[] elements, final Function<T, String> transform) {
        return Arrays.stream(elements).map(transform).collect(Collectors.joining(COLLECTION_SEPARATOR));
    }

    /**
     * Writes implementation for a given constructor.
     * @param constructor for which implementation is written
     * @param token       for which implementation is written
     * @return {@link String} containing implementation for a {@link Constructor} in generated class.
     */
    private String getConstructorBody(final Constructor constructor, final Class<?> token) {
        return elementsSpaced(elementsLineSeparated(tabs(1) + getClassName(token) +
                        getParameters(constructor),
                exceptionsOfExecutable(constructor) +
                        "{", tabs(2) + "super" +
                        getParametersNumbers(constructor)
                        + ";", "}"));
    }

    /**
     * return {@link Class#getSimpleName()} with word "Impl".
     * used to generate name for implemented class.
     * @param token instance of {@link Class}
     * @return String of Simple name for the answer.
     */
    String getClassName(final Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Joins elements with {@link #LINE_SEP}.
     *
     * @param blocks array of values to be joined together.
     * @return a string containing elements joined together
     * with {@link #LINE_SEP}, string ends with additional {@link #LINE_SEP}.
     */
    private String elementsLineSeparated(final String... blocks) {
        return String.join(LINE_SEP, blocks) + LINE_SEP;
    }

    /**
     * Makes a string consisting of code of implementation class.
     *
     * @param token class is generating implementation to.
     * @return {@link String} consisting of source code in generated class.
     * @throws ImplerException if implementation could not be generated.
     */
    private String getFullClass(final Class<?> token) throws ImplerException {
        final String extendsOrImplements = token.isInterface() ? "implements" : "extends";
        return elementsSeparated(System.lineSeparator(),
                emptyOrPrefix("package", token.getPackageName(), ";"),
                elementsSeparated(" ",  elementsSpaced(getClassModifiers(token),
                        "class", getClassName(token), extendsOrImplements,
                        token.getCanonicalName(), "{")),
                getConstructors(token),
                getMethods(token),
                "}");
    }

    /**
     * Joins elements with {@link #SPACE}.
     *
     * @param parts array of values to be joined together.
     * @return a string containing elements joined together
     * * with {@link #SPACE}.
     */
    private String elementsSpaced(final String... parts) {
        return String.join(SPACE, parts);
    }

    /**
     * Encodes a given {@link String}.
     *
     * @param arg encoding of which is needed
     * @return {@link String} which is encoded.
     */
    private static String encode(final String arg) {
        final StringBuilder builder = new StringBuilder();
        for (final char c : arg.toCharArray()) {
            builder.append(c < 128 ? String.valueOf(c) : String.format("\\u%04x", (int) c));
        }
        return builder.toString();
    }

    /**
     * Function used to create a {@code .java} file implementing methods in given class or interface.
     * to implement {@code token} class in location specified by {@code root}.
     *
     * @param token type token to create implementation for.
     * @param root  target future {@code .jar} file.
     * @throws ImplerException if {@link Path} is incorrect, could not create parent directories for a path
     *                         or {@link BufferedWriter} could not be created.
     */
    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Parameters should not be null");
        }
        final Path place;
        try {
            place = root.resolve(Path.of(token.getPackageName().replace('.', File.separatorChar),
                    getClassName(token) + ".java"));
            Files.createDirectories(place.getParent());
        } catch (final InvalidPathException | IOException e) {
            throw new ImplerException("Wrong path");
        }
        final int modifiers = token.getModifiers();
        if (token.isPrimitive()
                || token.isArray()
                || Modifier.isFinal(modifiers)
                || Modifier.isPrivate(modifiers)
                || token == Enum.class) {
            throw new ImplerException("Unsupported token given");
        }
        try (final BufferedWriter writer = Files.newBufferedWriter(place)) {
            writer.write(encode(getFullClass(token)));
        } catch (final IOException e) {
            throw new ImplerException(e);
        }
    }

}