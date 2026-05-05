package com.digifly.uart;

/**
 * uart 工具
 */

public class UartTool {

    private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public synchronized static String byteArrayToHexString(byte[] bytes, boolean isDebug) {
        if (isDebug) {
            StringBuilder builder = new StringBuilder();
            for (byte data : bytes) {
                builder.append(String.format("0x%x", data)).append(" ");
            }

            return builder.toString();
        } else {
            char[] hexChars = new char[bytes.length * 2];
            int v;

            for (int j = 0; j < bytes.length; j++) {
                v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexDigits[v >>> 4];
                hexChars[j * 2 + 1] = hexDigits[v & 0x0F];
            }

            return String.format("%s", new String(hexChars));
        }
    }
}
