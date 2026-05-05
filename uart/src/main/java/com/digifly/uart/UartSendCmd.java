package com.digifly.uart;

/**
 * uart app -> 設備 cmd
 */
public class UartSendCmd {

    public static byte[] FACTORY_SET_BATTERY_SHUTDOWN = new byte[]{0x55, 0x10, (byte) 0xaa};

    public static byte[] SHUTDOWN = new byte[]{0x55, 0x11, (byte) 0xaa};

    public static byte[] IDLE = new byte[]{0x55, 0x12, (byte) 0xaa};

    public static byte[] CANCEL = new byte[]{0x55, 0x00, (byte) 0xaa};

    public static byte[] GET_MCU_VERSION = new byte[]{(byte)0xb5, 0x5b, (byte) 0xaa};
}
