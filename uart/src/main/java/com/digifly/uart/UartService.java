package com.digifly.uart;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.digifly.uart.data.UartReceiveAckData;
import com.digifly.uart.data.UartReceiveBaseData;
import com.digifly.uart.listener.UartReceiveListener;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

/**
 * Uart Service
 */
public class UartService extends Service {

    private static final String TAG = UartService.class.getSimpleName();

    /**
     * 是否顯示 uart 接收到的 log
     */
    public static final boolean isShowReceiveDataLog = false;

    private final IBinder mBinder = new LocalBinder();

    /**
     * 發送命令列隊
     */
    private Deque<byte[]> writeDeque;

    /**
     * 是否執行 uart 傳送
     */
    private boolean isSending = false;

    /**
     * uart 傳送 Thread
     */
    @Nullable
    private SenderThread senderThread;

    /**
     * 串行端口 控制
     */
    @Nullable
    private SerialPortController serialPortController;

    /**
     * 背景 HandlerThread
     */
    @Nullable
    private HandlerThread workerHandlerThread = null;

    /**
     * 背景 workerHandler
     */
    @Nullable
    private Handler workerHandler = null;

    /**
     * uart 接收 Handler
     */
    @Nullable
    private ReceiveHandler receiveHandler;

    /**
     * 當下要發送的 uart cmd
     */
    @Nullable
    private byte[] lastCmd;

    /**
     * 最後收到的 uart 資料
     */
    @Nullable
    private UartReceiveBaseData nowUartReceiveBaseData = null;

    /**
     * 最後收到的 uart 資料 線程鎖
     */
    private final Object nowUartReceiveBaseDataLock = new Object();

    /**
     * 上次收到的 uart 資料
     */
    @Nullable
    private UartReceiveBaseData oldUartReceiveBaseData = null;

    /**
     * uart 接收資料聆聽 list
     */
    @NonNull
    private List<UartReceiveListener> uartReceiveListenerList = new ArrayList<>();

    /**
     * uart 接收資料聆聽 list 線程鎖
     */
    private final Object receiveListenerListLock = new Object();

    /**
     * 與 activity 綁 UartService
     */
    public static boolean bindService(@NonNull AppCompatActivity activity, @NonNull ServiceConnection serviceConnection) {
        try {
            activity.bindService(new Intent(activity, UartService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    /**
     * 與 activity 解綁 UartService
     */
    public static boolean unbindService(@NonNull AppCompatActivity activity, @NonNull ServiceConnection serviceConnection) {
        try {
            activity.unbindService(serviceConnection);
            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    public class LocalBinder extends Binder {

        @NonNull
        public UartService getUartService() {
            return UartService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        writeDeque = new LinkedList<>();
        isSending = true;
        if (senderThread == null) {
            Log.i(TAG, "onCreate senderThread is null ");
            senderThread = new SenderThread();
            senderThread.start();
        }

        if (workerHandlerThread == null) {
            workerHandlerThread = new HandlerThread(TAG + "_workerHandlerThread");
            workerHandlerThread.start();
        }

        if (workerHandler == null) {
            workerHandler = new Handler(workerHandlerThread.getLooper());
        }

        if (receiveHandler == null) {
            receiveHandler = new ReceiveHandler(workerHandlerThread.getLooper());
        }

        openUart();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG , "onBind");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG , "onUnbind");
        return super.onUnbind(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service Destroy: ");
        isSending = false;

        final  SerialPortController serialPortController = UartService.this.serialPortController;
        if (serialPortController != null) {
            serialPortController.stop();
        }
        UartService.this.serialPortController = null;

        final ReceiveHandler receiveHandler = UartService.this.receiveHandler;
        if (receiveHandler != null) {
            receiveHandler.removeCallbacksAndMessages(null);
        }
        UartService.this.receiveHandler = null;

        final Handler workerHandler = UartService.this.workerHandler;
        if (workerHandler != null) {
            workerHandler.removeCallbacksAndMessages(null);
        }
        UartService.this.workerHandler = null;
    }

    /**
     * 開啟 Uart
     */
    private void openUart() {
        if (serialPortController != null) {
            serialPortController.cleanInstance();
        }
        serialPortController = SerialPortController.getInstance();
        serialPortController.setReceiveHandler(receiveHandler);
        serialPortController.openUart();
    }

    /**
     * 送 uart cmd
     * 加到列隊
     */
    public synchronized void sendUartCmd(@NonNull final byte[] data) {
        if (data.length <= 0) {
            return;
        }
        synchronized (UartService.this) {
            try {
                writeDeque.add(data);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    /**
     * uart 傳送 Thread
     */
    private class SenderThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (isSending) {
                try {
                    try {
                        SystemClock.sleep(100);
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    synchronized (UartService.this) {
                        if (!writeDeque.isEmpty()) {
                            try {
                                lastCmd = writeDeque.poll();
                            } catch (Exception e) {
                                Timber.e(e);
                                writeDeque = new LinkedList<>();
                            }
                        }
                    }
                    if (lastCmd != null) {
                        SerialPortController serialPortController = UartService.this.serialPortController;
                        if (serialPortController != null) {
                            Timber.i("sendData =" + UartTool.byteArrayToHexString(lastCmd, true));
                            serialPortController.sendData(lastCmd);
                        }
                        lastCmd = null;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }
    }

    /**
     * uart 接收 Handler
     */
    private class ReceiveHandler extends Handler {

        public ReceiveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 1) {
                // 收到的資料
                byte[] receiveData = new byte[0];
                try {
                    receiveData = (byte[]) msg.obj;
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (receiveData != null && receiveData.length > 0) {

                    if (UartService.isShowReceiveDataLog) {
                        Timber.i("ReceiveHandler -> handleMessage -> receiveData=" + UartTool.byteArrayToHexString(receiveData, true));
                    }

                    @Nullable final UartReceiveBaseData uartReceiveBaseData = ParserUartReceive.parser(receiveData);

                    @Nullable UartReceiveAckData uartReceiveAckData = null;
                    if (uartReceiveBaseData == null) {
                        uartReceiveAckData = ParserUartReceive.parserAck(receiveData);
                    }

                    if (UartService.isShowReceiveDataLog) {
                        Timber.i("ReceiveHandler -> handleMessage -> uartReceiveBaseData=" + uartReceiveBaseData);
                        Timber.i("ReceiveHandler -> handleMessage -> uartReceiveAckData=" + uartReceiveAckData);
                    }

                    if (uartReceiveBaseData != null) {
                        setNowUartReceiveBaseData(uartReceiveBaseData);
                        final UartReceiveBaseData oldData = getOldUartReceiveBaseData();

                        final Handler workerHandler = UartService.this.workerHandler;
                        if (workerHandler != null) {
                            workerHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    List<UartReceiveListener> list = getReceiveListenerList();
                                    for (UartReceiveListener l : list) {
                                        l.onReceiveData(uartReceiveBaseData, oldData);
                                    }
                                }
                            });
                        }
                    } else if (uartReceiveAckData != null) {
                        final Handler workerHandler = UartService.this.workerHandler;
                        if (workerHandler != null) {
                            UartReceiveAckData finalUartReceiveAckData = uartReceiveAckData;
                            workerHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    List<UartReceiveListener> list = getReceiveListenerList();
                                    for (UartReceiveListener l : list) {
                                        l.onReceiveAckData(finalUartReceiveAckData);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    /**
     * 取 最後收到的 uart 資料
     */
    @Nullable
    public synchronized UartReceiveBaseData getNowUartReceiveBaseData() {
        synchronized (nowUartReceiveBaseDataLock) {
            return nowUartReceiveBaseData;
        }
    }

    /**
     * 設 最後收到的 uart 資料
     */
    private synchronized void setNowUartReceiveBaseData(@Nullable UartReceiveBaseData nowUartReceiveBaseData) {
        synchronized (nowUartReceiveBaseDataLock) {
            this.oldUartReceiveBaseData = this.nowUartReceiveBaseData;
            this.nowUartReceiveBaseData = nowUartReceiveBaseData;
        }
    }

    /**
     * 取 上次收到的 uart 資料
     */
    @Nullable
    public UartReceiveBaseData getOldUartReceiveBaseData() {
        synchronized (nowUartReceiveBaseDataLock) {
            return oldUartReceiveBaseData;
        }
    }

    /**
     * 加入 uart 接收資料聆聽
     */
    public synchronized void addReceiveListener(@NonNull final UartReceiveListener uartReceiveListener) {
        synchronized (receiveListenerListLock) {
            ArrayList<UartReceiveListener> uartReceiveListenerList = new ArrayList<>(UartService.this.uartReceiveListenerList);
            uartReceiveListenerList.add(uartReceiveListener);
            UartService.this.uartReceiveListenerList = uartReceiveListenerList;
        }
    }

    /**
     * 移除 uart 接收資料聆聽
     */
    public synchronized void removeReceiveListener(@NonNull final UartReceiveListener uartReceiveListener) {
        synchronized (receiveListenerListLock) {
            ArrayList<UartReceiveListener> uartReceiveListenerList = new ArrayList<>(UartService.this.uartReceiveListenerList);
            try {
                uartReceiveListenerList.remove(uartReceiveListener);
            } catch (Exception e) {
                Timber.e(e);
            }
            UartService.this.uartReceiveListenerList = uartReceiveListenerList;
        }
    }

    /**
     * 取 uart 接收資料聆聽 list
     */
    @NonNull
    public synchronized List<UartReceiveListener> getReceiveListenerList() {
        synchronized (receiveListenerListLock) {
            return new ArrayList<>(UartService.this.uartReceiveListenerList);
        }
    }
}
