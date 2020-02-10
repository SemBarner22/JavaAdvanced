package ru.ifmo.rain.zagretdinov.walk;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class HashWriter extends SimpleFileVisitor<Path> {
    private static final int FNV_32_INIT = 0x811c9dc5;
    private static final int FNV_32_PRIME = 0x01000193;
    private static final int HASH_ERROR = 0;
    private BufferedWriter bufferedWriter;

    HashWriter(BufferedWriter bufferedWriter) {
        super();
        this.bufferedWriter = bufferedWriter;
    }

    private static int hash32(BufferedInputStream bufferedInputStream) throws IOException {
        int rv = FNV_32_INIT;
        byte[] data = new byte[1024];
        int bytesRead = bufferedInputStream.read(data, 0, data.length);
        while (bytesRead != -1) {
            rv = hashCalc(data, bytesRead, rv);
            bytesRead = bufferedInputStream.read(data, 0, data.length);
        }
        return rv;
    }

    private static int hashCalc(byte[] data, int bytesRead, int rv) {
        for (int i = 0; i < bytesRead; i++) {
            rv *= FNV_32_PRIME;
            rv ^= (data[i] & 0xff);
        }
        return rv;
    }

    private FileVisitResult writeHash(Path path, boolean isFailed) {
        int hash = HASH_ERROR;
        if (!isFailed) {
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(
                    new FileInputStream(String.valueOf(path)))) {
                hash = hash32(bufferedInputStream);
            } catch (IOException e) {
                System.out.println("An error occurred during reading a file " + path);
            }
        }
        try {
            bufferedWriter.write(String.format("%08x %s\n", hash, path.toString()));
        } catch (IOException e) {
            System.out.println("An error occurred while trying to output hash and filename "
                    + path + " " + e.getMessage());
            return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
        return writeHash(path, false);
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        return writeHash(path, true);
    }
}