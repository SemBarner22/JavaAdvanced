package ru.ifmo.rain.zagretdinov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class RecursiveWalk {
    public static void main(String[] args) throws RecursiveWalkException{
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            if (args == null || args.length != 2) {
                System.err.println("Invalid amount of arguments! " +
                        "Usage: input filename output filename");
            } else if (args[1] == null) {
                System.err.println("Output file must not be null");
            } else {
                System.err.println("Input file must not be null");
            }
            throw new RecursiveWalkException("Arguments error");
        }
        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(args[1]), StandardCharsets.UTF_8)) {
                String nextReadString;
                while ((nextReadString = bufferedReader.readLine()) != null) {
                    Files.walkFileTree(Paths.get(nextReadString), new HashWriter(bufferedWriter));
                }
            } catch (IOException e) {
                System.out.println("An error occurred during writing to output file: " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found");
        } catch (IOException e) {
            System.out.println("An error occurred during reading from input file: " + e.getMessage());
        }
    }
}
