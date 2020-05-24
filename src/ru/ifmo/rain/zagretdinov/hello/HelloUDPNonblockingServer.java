package ru.ifmo.rain.zagretdinov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPNonblockingServer implements HelloServer {
    private int BUFFER_SIZE;
    private Selector selector;
    private DatagramChannel channel;
    private ExecutorService threadService;
    private ExecutorService worker;

    public static void main(final String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: [port], [threads amount]");
            return;
        }
        try (HelloUDPServer server = new HelloUDPServer()) {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            server.start(port, threads);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            reader.readLine();
        } catch (final NumberFormatException | IOException e) {
            System.err.println("Port and threads amount should be integer");
        }
    }

    private class BufferAndAddress {
        SocketAddress socketAddress;
        ByteBuffer byteBuffer;
        BufferAndAddress(ByteBuffer byteBuffer, SocketAddress socketAddress) {
           this.byteBuffer = byteBuffer;
           this.socketAddress = socketAddress;
        }
    }

    private class BufferContext {
        private List<ByteBuffer> inputBuffers;
        private List<BufferAndAddress> outputBuffers;
        BufferContext(int threadSize) {
            this.inputBuffers = new ArrayList<>(threadSize);
            this.outputBuffers = new ArrayList<>(threadSize);
            IntStream.range(0, threadSize).forEach(i -> inputBuffers.add(ByteBuffer.allocate(BUFFER_SIZE)));
        }


    }

    @Override
    public void start(int port, int threads) {
        try {
            worker = Executors.newSingleThreadExecutor();
            threadService = Executors.newFixedThreadPool(threads);
            channel = DatagramChannel.open();
            selector = Selector.open();
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(port));
            BUFFER_SIZE = channel.socket().getReceiveBufferSize();
            channel.setOption(StandardSocketOptions.SO_RCVBUF, BUFFER_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final BufferContext context = new BufferContext(threads);
        try {
            channel.register(selector, SelectionKey.OP_READ, context);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
        worker.submit(() -> {
            try {
                while (!Thread.interrupted() && !channel.socket().isClosed()) {
                    selector.select();

                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();
                        try {
                            if (key.isWritable()) {
                                handleWrite(key);
                            }
                            if (key.isReadable()) {
                                handleRead(key);
                            }
//                        } catch (final IOException e) {
                        } finally {
                            i.remove();
                        }
                    }
                }
            } catch (final IOException e) {
            } finally {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void handleRead(SelectionKey key) {

    }

    private void handleWrite(SelectionKey key) {
    }

    @Override
    public void close() {
        try {
            selector.close();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadService.shutdown();
        worker.shutdown();
        try {
            threadService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            // Ignored
        }
    }

//    while (!Thread.interrupted()) {
//        selector.select();
//        for (final Iterator<SelectionKey> i =
//             selector.selectedKeys().iterator(); i.hasNext(); ) {
//            final SelectionKey key = i.next();
//            try {
//                if (key.isAcceptable()) {
//                    final ServerSocketChannel serverChannel =
//                            (ServerSocketChannel) key.channel();
//                    handle(serverChannel.accept());
//                }
//            } finally {
//                i.remove();
//            }
//        }
}
