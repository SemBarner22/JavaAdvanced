package ru.ifmo.rain.zagretdinov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static ru.ifmo.rain.zagretdinov.implementor.ImplementorUtils.getImplementationPath;

// :NOTE: Использование <code> // DONE
/**
 * Class implements {@link JarImpler}. Provides methods to implement {@code  .jar} files
 * from given class or interface.
 *
 * @author sem
 * @version 1.0
 */
public class JarImplementor extends Implementor implements JarImpler {
    /**
     * Function used to determine whether generate {@code .jar} or {@code .java} file.
     * Continues working with {@link Implementor} in two different scenarios:
     * <ul>
     * <li> 2 arguments {@code className outputPath}: creates {@code .java} file executing
     * method {@link #implement(Class, Path)} provided by interface {@link Impler} </li>
     * <li> 3 arguments {@code -jar className outputPath}: creates {@code .jar} file executing
     * * method {@link #implementJar(Class, Path)} provided by interface {@link JarImpler} </li>
     * </ul>
     * Arguments should not be null. If input is incorrect or an error happens during executing
     * message is printed and execution is aborted.
     * @param args console line arguments: {@code [-jar] className outputPath}
     */
    public static void main(final String[] args) {
        if (args == null || args.length < 2 || args.length > 3) {
            System.err.println("Invalid arguments number, expected [-jar] <class.name> <output.path>");
        } else {
            for (final String arg : args) {
                if (arg == null) {
                    System.err.println("All arguments should be not null");
                    return;
                }
            }
            try {
                if (args.length == 2) {
                    new Implementor().implement(Class.forName(args[0]), Path.of(args[1]));
                } else if (args[0].equals("-jar") || args[0].equals("--jar")) {
                    new JarImplementor().implementJar(Class.forName(args[1]), Path.of(args[2]));
                } else {
                    System.err.println("expected -jar or --jar");
                }
            } catch (final ClassNotFoundException e) {
                System.err.println("Invalid class name given: " + e.getMessage());
            } catch (final InvalidPathException e) {
                System.err.println("Invalid path given: " + e.getMessage());
            } catch (final ImplerException e) {
                System.err.println("Error while creating " +
                        ((args.length == 2) ? "java" : "jar") + " file " + e.getMessage());
            }
        }
    }

    /**
     * Method for compiling created {@code .jar} file. Finds a {@link JavaCompiler},
     * runs it with a command to compile generated class.
     * @param token  type token to create implementation for.
     * @param tmpDir {@link Path} for a temporary directory used for creating a compiled
     *               {@code .jar} class.
     * @throws ImplerException if {@link JavaCompiler} could not be find or it could not be run.
     */
    private void compileClass(final Class<?> token, final Path tmpDir) throws ImplerException {
        final String subPath;
        try {
            subPath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("Failed to retrieve location path");
        }

        final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            throw new ImplerException("No Java compiler provided");
        }

        final String[] compilerArgs = {
                "-cp",
                subPath,
                tmpDir.resolve(getImplementationPath(token, File.separator) + "Impl.java").toString(),
        };

        final int returnCode = javaCompiler.run(null, null, null, compilerArgs);
        if (returnCode != 0) {
            throw new ImplerException("Implementation compilation returned non-zero code " + returnCode);
        }
    }


    /**
     * Creates a {@code .jar} file containing implementation for a given class or interface.
     * Creates a {@link Manifest} for an {@code .jar} file.
     * @param jarFile target {@code .jar} file.
     * @param tempDirectory {@link Path} for a temporary directory used for building a compiled {@code .jar} class.
     * @param token {@link Class} token to create implementation for.
     * @throws ImplerException if {@link JarOutputStream} could not be created.
     */
    private void buildJar(final Path jarFile, final Path tempDirectory, final Class<?> token) throws ImplerException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (final JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            final String implementationPath = getImplementationPath(token, "/") + "Impl.class";
            stream.putNextEntry(new ZipEntry(implementationPath));
            Files.copy(Path.of(tempDirectory.toString(), implementationPath), stream);
        } catch (final IOException e) {
            throw new ImplerException("Failed to write JAR", e);
        }
    }

    /**
     * Function used to create a compiled {@code .jar} file implementing methods in given class or interface.
     * Uses {@link #compileClass(Class, Path)} to compile a file, {@link #implement(Class, Path)}
     * to implement {@code  token} class in location specified by {@code  jarFile}
     * @param token   {@link Class} to create implementation for.
     * @param jarFile target future {@code .jar} file.
     * @throws ImplerException if {@link Class} or {@link Path} is null.
     */
    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Invalid (null) argument given");
        }
        ImplementorUtils.createDirectoriesTo(jarFile.normalize());
        final ImplementorUtils utils = new ImplementorUtils(jarFile.toAbsolutePath().getParent());
        implement(token, utils.getTmpDir());
        compileClass(token, utils.getTmpDir());
        buildJar(jarFile, utils.getTmpDir(), token);

    }
}