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

    private static int hash32(Path path) throws IOException, NullPointerException {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            int res = FNV_32_INIT;
            byte[] data = new byte[1024];
            int bytesRead;
            while (true) {
                bytesRead = inputStream.read(data, 0, data.length);
                if (bytesRead == -1) {
                    break;
                }
                res = hashCalc(data, bytesRead, res);
            }
            return res;
        }
    }

    private static int hashCalc(byte[] data, int bytesRead, int res) {
        for (int i = 0; i < bytesRead; i++) {
            res *= FNV_32_PRIME;
            res ^= (data[i] & 0xff);
        }
        return res;
    }

    FileVisitResult writeHash(String string, boolean isFailed) throws IOException {
        int hash = HASH_ERROR;
        if (!isFailed) {
            Path path = null;
            try {
                path = Paths.get(string);
                try {
                    hash = hash32(path);
                } catch (IOException | NullPointerException e) {
                    System.err.println("An error occurred during reading a file " + path);
                }
            } catch (InvalidPathException e) {
                System.err.println("Impossible to get path from: " + string + " " + e.getMessage());
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