package ru.ifmo.rain.zagretdinov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class - Server implementing {@link HelloServer} interface. Receives and answers on requests in several threads,
 * prepending {@link String "Hello, "} to answers.
 */
public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService threadService;

    public static void main(String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: [port], [threads amount]");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            int threads = Integer.parseInt(args[1]);
            new HelloUDPServer().start(port, threads);
        } catch (NumberFormatException e) {
            System.err.println("Port and threads amount should be integer");
        }
    }

    @Override
    public void start(int port, int threads) {
        int size;
        if (port < 0 || threads <= 0) {
            System.err.println("Port and threads amount should be positive");
            return;
        }
        try {
            socket = new DatagramSocket(port);
            size = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.out.println("Could not establish a connection " + e.getMessage());
            return;
        }
        threadService = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            threadService.submit(() -> {
                try {
                    DatagramPacket packet = Utilities.newPacket(size);
                    while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                        socket.receive(packet);
                        Utilities.setContent(packet, "Hello, " + Utilities.getContent(packet));
                        socket.send(packet);
                    }
                }  catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println("An error during receiving or sending a message " + e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public void close() {
        socket.close();
        threadService.shutdown();
        try {
            threadService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignored
        }
    }
}