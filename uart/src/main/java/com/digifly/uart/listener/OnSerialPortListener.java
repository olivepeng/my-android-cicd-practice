package com.digifly.uart.listener;

/**
 * 串行端口控制 結構
 */

public interface OnSerialPortListener {

    void openUart();

    void sendData(byte[] data);

    void stop();
}
