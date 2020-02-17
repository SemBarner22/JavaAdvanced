package ru.ifmo.rain.zagretdinov.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileHashVisitor extends SimpleFileVisitor<Path> {
    private static final int FNV_32_INIT = 0x811c9dc5;
    private static final int FNV_32_PRIME = 0x01000193;
    private static final int HASH_ERROR = 0;
    private BufferedWriter bufferedWriter;

    FileHashVisitor(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    private static int hash32(Path path) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            int rv = FNV_32_INIT;
            byte[] data = new byte[1024];
            int bytesRead = inputStream.read(data, 0, data.length);
            while (bytesRead != -1) {
                rv = hashCalc(data, bytesRead, rv);
                bytesRead = inputStream.read(data, 0, data.length);
            }
            return rv;
        }
    }

    private static int hashCalc(byte[] data, int bytesRead, int rv) {
        for (int i = 0; i < bytesRead; i++) {
            rv *= FNV_32_PRIME;
            rv ^= (data[i] & 0xff);
        }
        return rv;
    }

    FileVisitResult writeHash(String string, boolean isFailed) throws IOException{
        int hash = HASH_ERROR;
        if (!isFailed) {
            Path path = null;
            try {
                path = Paths.get(string);
            } catch (InvalidPathException e) {
                System.err.println("Impossible to get path from: " + string + " " + e.getMessage());
            }
            try {
                hash = hash32(path);
            } catch (IOException e) {
                System.err.println("An error occurred during reading a file " + path);
            }
        }
        bufferedWriter.write(String.format("%08x %s%n", hash, string));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        return writeHash(path.toString(), false);
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
        return writeHash(path.toString(), true);
    }
}