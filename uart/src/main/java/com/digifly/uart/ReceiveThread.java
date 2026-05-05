package com.digifly.uart;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import timber.log.Timber;

/**
 * uart 接收 執行序
 */
public class ReceiveThread extends Thread {

    private static final String TAG = ReceiveThread.class.getSimpleName();

    @NonNull
    private final FileInputStream mInputStream;
    private boolean isStopped = false;
    private int receiveBufferLength;

    @NonNull
    private final byte[] receiveBuffer;

    @NonNull
    private final ByteArrayOutputStream temporaryStream;

    @NonNull
    private final Handler handler;

    ReceiveThread(@NonNull FileInputStream mInputStream,@NonNull  Handler receiveHandler) {
        this.mInputStream = mInputStream;
        this.handler = receiveHandler;

        isStopped = false;
        receiveBufferLength = 0;
        receiveBuffer = new byte[1024];
        temporaryStream = new ByteArrayOutputStream();
    }

    @Override
    public void run() {
        super.run();
        byte[] trackDataBytes;

        while(!Thread.currentThread().isInterrupted() && !isStopped) {

            try {
                if (mInputStream == null) {
                    isStopped = true;
                    return;
                }

                receiveBufferLength = 0;
                while ((receiveBufferLength = mInputStream.read(receiveBuffer)) != -1) {
                    // Write Data to Bytes Array Temporary Output Stream
                    temporaryStream.reset();
                    temporaryStream.write(receiveBuffer, 0, receiveBufferLength);
                    trackDataBytes = temporaryStream.toByteArray();
//                    isStopped = true;

                    String trackDataBytesHexString = UartTool.byteArrayToHexString(trackDataBytes, true);
                    if (UartService.isShowReceiveDataLog) {
                        Log.d(TAG, "trackDataBytesHexString=" + trackDataBytesHexString);
                    }


                    try {
                        if (
                                (((trackDataBytes[0] & 0xff) == 0xaa) &&
                                ((trackDataBytes[trackDataBytes.length - 1] & 0xff) == 0x55) &&
                                trackDataBytes.length == 9) ||
                                        (((trackDataBytes[0] & 0xff) == 0xa5) &&
                                                ((trackDataBytes[trackDataBytes.length - 1] & 0xff) == 0xaa) &&
                                                (trackDataBytes.length == 3 || trackDataBytes.length == 7))

                        ) {  //  資料正常
                            parse(trackDataBytes);
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    try {
                        SystemClock.sleep(10);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }


            } catch (Exception e) {
                Timber.e(e);
                isStopped = true;
            }
        }
    }

    private void parse(byte [] data) {
        if (handler == null){
            return;
        }
        if (data.length > 1) {
            Message message = new Message();
            message.what = 1;
            message.obj = data;
            handler.sendMessage(message);
        }
    }

    public void setStopped(boolean stopped) {
        isStopped = stopped;
    }
}
