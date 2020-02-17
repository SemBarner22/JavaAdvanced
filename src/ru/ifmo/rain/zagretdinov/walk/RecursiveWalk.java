package ru.ifmo.rain.zagretdinov.walk;

import java.io.*;
import java.nio.file.*;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            if (args == null || args.length != 2) {
                System.err.println("Invalid amount of arguments! " +
                        "Usage: input filename output filename");
            } else if (args[1] == null) {
                System.err.println("Output file must not be null");
            } else {
                System.err.println("Input file must not be null");
            }
            return;
        }
        Path path0;
        Path path1;
        try {
            path0 = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            System.out.println("An error occurred during creating input path: " + e.getMessage());
            return;
        }
        try {
            path1 = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.out.println("An error occurred during creating output path: " + e.getMessage());
            return;
        }
        try {
            run(path0, path1);
        } catch (WalkException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void run(Path path0, Path path1) throws WalkException {
        try (BufferedReader bufferedReader = Files.newBufferedReader(path0)) {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path1)) {
                String nextReadString;
                FileHashVisitor fileHashVisitor = new FileHashVisitor(bufferedWriter);
                while (true) {
                    try {
                        nextReadString = bufferedReader.readLine();
                        if (nextReadString == null) {
                            break;
                        }
                    } catch (IOException e) {
                        throw new WalkException("An error occurred during read from input file: " + e.getMessage(), e);
                    }
                    try {
                        Path nextPath = Paths.get(nextReadString);
                        try {
                            Files.walkFileTree(nextPath, fileHashVisitor);
                        } catch (IOException e) {
                            throw new WalkException("An error occurred while trying to output hash and filename to " +
                                  path1.toString() + e.getMessage());
                        }
                    } catch (InvalidPathException e) {
                        fileHashVisitor.writeHash(nextReadString, true);
                        throw new WalkException("Impossible to create path: " + e.getMessage(), e);
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

