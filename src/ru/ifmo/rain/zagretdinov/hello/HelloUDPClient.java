package ru.ifmo.rain.zagretdinov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Class - Client implementing {@link HelloClient} interface. Sends requests in several thread, each thread
 * sends several of them.
 */
public class HelloUDPClient implements HelloClient {
    private SocketAddress address;
    private ExecutorService clientThreads;
    private final int TIMEOUT_TIME = 100;

    public static void main(String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: [host], [port], [prefix], [threads], [requests]");
            return;
        }
        try {
            int port = Integer.parseInt(args[1]);
            int threads = Integer.parseInt(args[3]);
            int requests = Integer.parseInt(args[4]);
            new HelloUDPClient().run(args[0], port, args[2], threads, requests);
        } catch (NumberFormatException e) {
            System.err.println("Port, threads and request per thread amount should be integer");
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        if (port <= 0 || threads <= 0 || requests <= 0) {
            System.err.println("Port, threads and request per thread amount should be positive");
            return;
        }
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Could not connect to given host name and port " + e.getMessage());
        }
        clientThreads = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(i ->
                clientThreads.submit(() ->
                        workPerThread(prefix, i, requests)));
        clientThreads.shutdown();
        try {
            clientThreads.awaitTermination(requests * threads, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Procession have been interrupted " + e.getMessage());
        }

    }

    private void workPerThread(String prefix, int index, int requests) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_TIME);
            int size = socket.getReceiveBufferSize();
            final DatagramPacket packet = Utilities.newPacket(size, address);
            IntStream.range(0, requests).forEach(i -> {
                String string = Utilities.encode(prefix, index, i);
                while (!socket.isClosed() && !Thread.interrupted()) {
                    try {
                        Utilities.setContent(packet, string);
                        socket.send(packet);
                        packet.setData(new byte[size]);
                        socket.receive(packet);
                        String answer = Utilities.getContent(packet);
                        if (Utilities.validate(answer, index, i)) {
                            System.out.println(answer);
                            break;
                        }
                    } catch (IOException e) {
                        if (!socket.isClosed()) {
                            System.err.println("An error during receiving or sending a message " + e.getMessage());
                        }
                    }
                }
            });
        } catch (SocketException e) {
            System.err.println("Could not establish connection with server on some requests: " + e.getMessage());
        }
    }
}