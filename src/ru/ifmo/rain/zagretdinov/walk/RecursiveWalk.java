package ru.ifmo.rain.zagretdinov.walk;

import java.io.*;
import java.nio.file.*;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Invalid amount of arguments! " +
                    "Usage: input filename output filename");
            return;
        } else if (args[1] == null) {
            System.err.println("Output file must not be null");
            return;
        } else if (args[0] == null) {
            System.err.println("Input file must not be null");
            return;
        }

        Path pathInput, pathOutput;
        try {
            pathInput = pathGetter(args[0]);
            pathOutput = pathGetter(args[1]);
            try {
                Path parentDirectory = pathOutput.getParent();
                if (parentDirectory != null) {
                    Files.createDirectories(parentDirectory);
                }
            } catch (IOException e) {
                System.out.println("Could not make directory for output file: " + e.getMessage());
                return;
            }
        } catch (WalkException e) {
            System.out.println("An error occurred during creating input path: " + e.getMessage());
            return;
        }

        try {
            run(pathInput, pathOutput);
        } catch (WalkException e) {
            System.out.println(e.getMessage());
        }
    }

    private static Path pathGetter(String string) throws WalkException {
        try {
            return Paths.get(string);
        } catch (InvalidPathException e) {
            throw new WalkException("An error occurred during creating path: " + string + " " + e.getMessage());
        }
    }

    private static void run(Path pathInput, Path pathOutput) throws WalkException {
        try (BufferedReader bufferedReader = Files.newBufferedReader(pathInput)) {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(pathOutput)) {
                FileHashVisitor fileHashVisitor = new FileHashVisitor(bufferedWriter);
                while (true) {
                    String line;
                    try {
                        line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        }
                    } catch (IOException e) {
                        throw new WalkException("An error occurred during read from input file: " + e.getMessage(), e);
                    }
                    try {
                        Path nextPath = Paths.get(line);
                        try {
                            Files.walkFileTree(nextPath, fileHashVisitor);
                        } catch (IOException e) {
                            throw new WalkException("An error occurred while trying to output hash and filename to " +
                                    pathOutput.toString() + " " + e.getMessage());
                        }
                    } catch (InvalidPathException e) {
                        try {
                            fileHashVisitor.writeHash(line, true);
                        } catch (IOException ex) {
                            throw new WalkException("An error occurred during writing hash file: "
                                    + line + " " + ex.getMessage(), ex);
                        }
                    }
                }
            } catch (IOException e) {
                throw new WalkException("An error occurred during writing to output file: " + e.getMessage(), e);
            }
        } catch (FileNotFoundException e) {
            throw new WalkException("Input file not found: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new WalkException("An error occurred during read from input file: " + e.getMessage(), e);
        }
    }
}

