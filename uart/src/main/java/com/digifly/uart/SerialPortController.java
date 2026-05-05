package com.digifly.uart;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.digifly.uart.listener.OnSerialPortListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android_serialport_api.SerialPort;
import timber.log.Timber;

/**
 * 串行端口控制
 */
public class SerialPortController implements OnSerialPortListener {

    private static final String TAG = SerialPortController.class.getSimpleName();
    private static SerialPortController mInstance;

    @Nullable
    private SerialPort serialPort = null;

    @Nullable
    private FileOutputStream mOutputStream = null;

    @Nullable
    private Handler receiveHandler = null;

    /**
     * uart 接收 執行序
     */
    @Nullable
    private ReceiveThread receiveThread = null;

    private SerialPortController() {

    }

    public static synchronized SerialPortController getInstance() {
        if (mInstance == null) {
            mInstance = new SerialPortController();
        }
        return mInstance;
    }

    public void cleanInstance() {
        mInstance = null;
    }

    public void setReceiveHandler(@Nullable Handler receiveHandler) {
        this.receiveHandler = receiveHandler;
    }

    @Override
    public void openUart() {
        try {
            stop();

            serialPort = new SerialPort(new File("/dev/ttyS0"), 9600, 0);

            FileInputStream mInputStream = (FileInputStream) serialPort.getInputStream();
            mOutputStream = (FileOutputStream) serialPort.getOutputStream();

            if (receiveThread != null && receiveThread.isAlive()) {
                try {
                    receiveThread.setStopped(true);
                    receiveThread.join();
                } catch (InterruptedException e) {
                    Timber.w(TAG, "Interrupted! "+ e.getMessage());
                    /* Clean up whatever needs to be handled before interrupting  */
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Timber.e(e);
                }
            }

            if (receiveHandler != null) {
                receiveThread = new ReceiveThread(mInputStream, receiveHandler);
                receiveThread.start();
            }
            Timber.d("start");
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public synchronized void sendData(@NonNull byte[] data) {
        setData(data);
    }

    @Override
    public void stop() {

        if (mOutputStream != null) {
            try {
                mOutputStream.flush();
            } catch (Exception e) {
                Timber.e(e);
            }
            try {
                mOutputStream.close();
            } catch (Exception e) {
                Timber.e(e);
            }
            mOutputStream = null;
        }

        if (serialPort != null) {
            try {
                serialPort.getInputStream().close();
            } catch (Exception e) {
                Timber.e(e);
            }
            try {
                serialPort.close();
            } catch (Exception e) {
                Timber.e(e);
            }
            serialPort = null;
        }

        if (receiveThread != null) {
            try {
                receiveThread.setStopped(true);
                receiveThread.join();
            } catch (InterruptedException e) {
                Timber.w(TAG, "Interrupted! "+ e.getMessage());
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Timber.e(e);
            }
            receiveThread = null;
        }
    }

    private synchronized void setData(@NonNull byte[] data) throws NullPointerException {
        if (mOutputStream == null) {
//            throw new NullPointerException("No outputStream");
            return;
        }
        try {
            Timber.i("setData data=" + UartTool.byteArrayToHexString(data, true));
            mOutputStream.write(data);
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}