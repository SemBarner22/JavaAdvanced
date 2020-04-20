package ru.ifmo.rain.zagretdinov.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Class providing tools for request generation and {@link DatagramPacket} management.
 */
class Utilities {
    static DatagramPacket newPacket(int receiveSize) {
        return new DatagramPacket(new byte[receiveSize], receiveSize);
    }

    static DatagramPacket newPacket(int receiveSize, SocketAddress address) {
        return new DatagramPacket(new byte[receiveSize], receiveSize, address);
    }

    static boolean validate(String s, int threadId, int requestId) {
        return s.matches("[\\D]*" + threadId + "[\\D]*" + requestId + "[\\D]*");
    }

    static void setContent(DatagramPacket packet, String s) {
       packet.setData(s.getBytes(StandardCharsets.UTF_8));
    }

    static String encode(String prefix, int index, int reqId) {
        return prefix + index + "_" + reqId;
    }

    static String getContent(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }
}
