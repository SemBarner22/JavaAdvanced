package ru.ifmo.rain.zagretdinov.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Class providing tools for request generation and {@link DatagramPacket} management.
 */
class Utilities {

    static boolean validate(final String s, final int threadId, final int requestId) {
        final String threadIdStr = Integer.toString(threadId);
        final String requestIdStr = Integer.toString(requestId);
        final int l1 = skipCharacters(0, s, false);
        final int r1 = skipCharacters(l1, s, true);
        final int l2 = skipCharacters(r1, s, false);
        final int r2 = skipCharacters(l2, s, true);
        return threadIdStr.equals(s.substring(l1, r1)) && requestIdStr.equals(s.substring(l2, r2));
    }

    private static int skipCharacters(int pos, final String s, final boolean skipDigits) {
        while (pos < s.length() && skipDigits == Character.isDigit(s.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    static void setContent(final DatagramPacket packet, final String s, final SocketAddress address) {
        packet.setData(s.getBytes(StandardCharsets.UTF_8));
        packet.setSocketAddress(address);
    }

    static void setContent(final DatagramPacket packet, final String s) {
        packet.setData(s.getBytes(StandardCharsets.UTF_8));
    }

    static String encode(final String prefix, final int index, final int reqId) {
        return prefix + index + "_" + reqId;
    }

    static String getContent(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

}
