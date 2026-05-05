package com.miis.horusendoview.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.miis.horusendoview.manager.MyStorageManager;

import java.io.File;

import timber.log.Timber;

public class StorageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("StorageReceiver", "onReceive  action=" + intent.getAction());

        try {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                String usbDrivePath = intent.getData() != null ? intent.getData().getPath() : null;
                if (usbDrivePath != null) {
                    Log.d("StorageReceiver", "ACTION_MEDIA_MOUNTED Storage drive detected at " + usbDrivePath);
                    File storageFile = new File(usbDrivePath);
                    MyStorageManager.getInstance().addStorage(storageFile);
                    MyStorageManager.getInstance().sendOnStorageMounted(storageFile);
                }
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action) || Intent.ACTION_MEDIA_EJECT.equals(action)) {
                String usbDrivePath = intent.getData() != null ? intent.getData().getPath() : null;
                if (usbDrivePath != null) {
                    Log.d("StorageReceiver", "ACTION_MEDIA_EJECT Storage drive detected at " + usbDrivePath);
                    File storageFile = new File(usbDrivePath);
                    MyStorageManager.getInstance().removeStorage(storageFile);
                    MyStorageManager.getInstance().sendOnStorageUnmounted(storageFile);
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}

