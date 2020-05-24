package ru.ifmo.rain.zagretdinov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Class - Server implementing {@link HelloServer} interface. Receives and answers on requests in several threads,
 * prepending {@link String "Hello, "} to answers.
 */
public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService threadService;

    public static void main(final String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: [port], [threads amount]");
            return;
        }
        try (HelloUDPServer server = new HelloUDPServer()) {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            server.start(port, threads);

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Server has started, press any button to stop");
            reader.readLine();
        } catch (final NumberFormatException e) {
            System.err.println("Port and threads amount should be integer");
        } catch (IOException e) {
            System.err.println("An error happened during reading: " + e.getMessage());
        }
    }

    @Override
    public void start(final int port, final int threads) {
        final int size;
        try {
            socket = new DatagramSocket(port);
            size = socket.getReceiveBufferSize();
        } catch (final SocketException e) {
            System.out.println("Could not establish a connection " + e.getMessage());
            return;
        }
        threadService = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(i -> threadService.submit(() -> {
            final byte[] buffer = new byte[size];
            // :NOTE: Переиспользование // Done
            final DatagramPacket packet = new DatagramPacket(buffer, size);
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    packet.setData(buffer);
                    socket.receive(packet);
                    Utilities.setContent(packet, "Hello, " + Utilities.getContent(packet));
                    socket.send(packet);
                } catch (final IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println("An error during receiving or sending a message " + e.getMessage());
                    }
                }
            }
        }));
    }

    @Override
    public void close() {
        socket.close();
        threadService.shutdown();
        try {
            threadService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            // Ignored
        }
    }
}