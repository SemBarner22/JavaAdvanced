package ru.ifmo.rain.zagretdinov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.IntStream.range;

public class HelloUDPClient implements HelloClient {
    private SocketAddress address;
    private ExecutorService clientThreads;

    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Usage: port and amount of threads");
            return;
        }
        // number checker
        new HelloUDPClient().run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
            parallelWork(prefix, threads, requests);
        } catch (UnknownHostException e) {
            System.err.println("Socket error occurred1: " + e.getMessage());
        }
    }

    private void parallelWork(String prefix, int threads, int requests) {
        clientThreads = Executors.newFixedThreadPool(threads);
        range(0, threads).forEach(i ->
            clientThreads.submit(() ->
                    workPerThread(prefix, i, requests)));
        clientThreads.shutdown();
        try {
            clientThreads.awaitTermination(5 * requests * threads, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Socket error occurred2: " + e.getMessage());
        }
    }

    private void workPerThread(String prefix, int index, int requests) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(200);
            int size = socket.getReceiveBufferSize();
            final DatagramPacket packet = new DatagramPacket(new byte[size], size, address);
            range(0, requests).forEach(i -> {
//            for (int i = 0; i < requests; i++) {
                String string = Utilities.encode(prefix, index, i);
                while (!socket.isClosed() && !Thread.interrupted()) {
//                    Utilities.newPacketFromString(string, address);
                    Utilities.setContent(packet, string);
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        System.err.println("Socket error occurred2: " + e.getMessage());
                    }
                    packet.setData(new byte[size]);
                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        System.err.println("Socket error occurred2: " + e.getMessage());
                    }
                    String answer = Utilities.getContent(packet);
                    if (!answer.contains(string)) {
                        continue;
                    }
                    System.out.println(answer);
                    break;
                }
            });
        } catch (SocketException e) {
            System.err.println("Socket error occurred2: " + e.getMessage());
        }
    }
}
