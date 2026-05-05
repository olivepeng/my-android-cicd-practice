package com.miis.horusendoview.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.type.StandbyNotificationTimeType;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class StandbyNotificationTimeService extends Service {

    /**
     * 與 activity 綁 UartService
     */

    public static boolean bindService(
            @NonNull AppCompatActivity activity,
            @NonNull ServiceConnection serviceConnection
    ) {
        try {
            activity.bindService(
                    new Intent(activity, StandbyNotificationTimeService.class),
                    serviceConnection,
                    Context.BIND_AUTO_CREATE
            );
            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    /**
     * 與 activity 解綁 UartService
     */
    public static boolean unbindService(
            @NonNull AppCompatActivity activity,
            @NonNull ServiceConnection serviceConnection
    ) {
        try {
            activity.unbindService(serviceConnection);
            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    @NonNull
    private final IBinder localBinder = new LocalBinder();

    /**
     * 聆聽 list
     */
    @NonNull
    private List<Listener> listenerList = new ArrayList<>();
    private final Object listenerListLock = new Object();

    /**
     * 倒數計時器
     */
    @Nullable
    private CountDownTimer countDownTimer = null;
    private final Object countDownTimerLock = new Object();

    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
        mainHandler = new Handler(Looper.getMainLooper());

        startCountDownTimer();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("onBind");
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopCountDownTimer();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        stopCountDownTimer();
        super.onDestroy();
    }

    /**
     * 開始 數計時器
     */
    public synchronized void startCountDownTimer() {
        stopCountDownTimer();
        mainHandler.post((new Runnable() {
            @Override
            public void run() {
                synchronized (countDownTimerLock) {
                    stopCountDownTimer();
                    StandbyNotificationTimeType type = SharedPreferencesManager.getInstance().getStandbyNotificationTimeType();
                    if (type == null) {
                        type = StandbyNotificationTimeType.NEVER;
                    }
                    long sec = 0;
                    switch (type) {
                        case MIN_10:
                            sec = (10 * 60);
                            break;
                        case MIN_30:
                            sec = (30 * 60);
                            break;
                        case MIN_60:
                            sec = (60 * 60);
                            break;
                        case NEVER:
                            sec = 0;
                            break;
                    }
                    sec -= 60;
                    if (sec <= 0) {
                        return;
                    }

                    final long finalSec = sec;

                    final CountDownTimer countDownTimer = new CountDownTimer(finalSec * 1000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            Timber.d("countDownTimer -> onTick millisUntilFinished=" + Math.round(millisUntilFinished / 1000f) + "sec");
                        }

                        @Override
                        public void onFinish() {
                            Timber.d("countDownTimer -> onFinish");
                            final StandbyNotificationTimeType type = SharedPreferencesManager.getInstance().getStandbyNotificationTimeType();
                            if (type == StandbyNotificationTimeType.NEVER) {
                                return;
                            }
                            List<Listener> list = getListenerList();
                            for (Listener listener : list) {
                                listener.onTimeout();
                            }
                        }
                    };

                    countDownTimer.start();
                    StandbyNotificationTimeService.this.countDownTimer = countDownTimer;
                }
            }
        }));
    }


    /**
     * 停止 數計時器
     */
    public synchronized void stopCountDownTimer() {
        synchronized (countDownTimerLock) {
            final CountDownTimer countDownTimer = StandbyNotificationTimeService.this.countDownTimer;
            if (countDownTimer != null) {
                countDownTimer.cancel();
                StandbyNotificationTimeService.this.countDownTimer = null;
            }
        }
    }

    /**
     * 加入 聆聽
     */
    public synchronized void addListener(@NonNull Listener listener) {
        synchronized (listenerListLock) {
            final List<Listener> list = new ArrayList<>(listenerList);
            list.add(listener);
            listenerList = list;
        }
    }

    /**
     * 移除 聆聽
     */
    public synchronized void removeListener(@NonNull Listener listener) {
        synchronized (listenerListLock) {
            final List<Listener> list = new ArrayList<>(listenerList);
            try {
                list.remove(listener);
            } catch (Exception e) {
                Timber.e(e);
            }
            listenerList = list;
        }
    }


    /**
     * 取 聆聽 list
     */
    @NonNull
    public synchronized List<Listener> getListenerList() {
        synchronized (listenerListLock) {
            return new ArrayList<>(listenerList);
        }
    }


    public class LocalBinder extends Binder {
        public StandbyNotificationTimeService getStandbyNotificationTimeService() {
            return StandbyNotificationTimeService.this;
        }
    }


    public interface Listener {
        /**
         * 倒數時間前 60秒 到
         */
        @WorkerThread
        void onTimeout();
    }


}
