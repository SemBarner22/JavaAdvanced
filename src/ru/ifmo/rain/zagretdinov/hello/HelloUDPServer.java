package ru.ifmo.rain.zagretdinov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService threadService;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: port and amount of threads");
            return;
        }
        // number checker
        HelloServer server = new HelloUDPServer();
        server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }

    @Override
    public void start(int port, int threads) {
        threadService = Executors.newFixedThreadPool(threads);
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < threads; i++) {
            threadService.submit(() -> {
                try {
                    DatagramPacket packet = Utilities.newPacket(socket.getReceiveBufferSize());
                    while (!socket.isClosed() && !threadService.isTerminated()) {
                        socket.receive(packet);
                        Utilities.setContent(packet, "Hello, " + Utilities.getContent(packet));
                        socket.send(packet);
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    @Override
    public void close() {

    }
}
