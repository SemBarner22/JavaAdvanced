package ru.ifmo.rain.zagretdinov.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class Utilities {
    static DatagramPacket newPacket(int receiveSize) {
        return new DatagramPacket(new byte[receiveSize], receiveSize);
    }

    static DatagramPacket newPacketFromString(String string, SocketAddress address) {
        return new DatagramPacket(string.getBytes(StandardCharsets.UTF_8), string.length(), address);
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
