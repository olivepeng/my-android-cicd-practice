package com.digifly.uart.listener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.digifly.uart.data.UartReceiveAckData;
import com.digifly.uart.data.UartReceiveBaseData;

/**
 * uart 接收資料聆聽
 */
public interface UartReceiveListener {

    /**
     * uart 收到已解析處理資料
     */
    @WorkerThread
    void onReceiveData(@NonNull UartReceiveBaseData uartReceiveBaseData, @Nullable UartReceiveBaseData oldUartReceiveBaseData);

    @WorkerThread
    void onReceiveAckData(@NonNull UartReceiveAckData uartReceiveAckData);
}
