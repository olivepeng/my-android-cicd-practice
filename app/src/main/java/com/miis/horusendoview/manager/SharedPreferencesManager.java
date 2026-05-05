package com.miis.horusendoview.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.type.BootAutoDeleteFileType;
import com.miis.horusendoview.type.ImageRotateType;
import com.miis.horusendoview.type.MaximumVideoDurationType;
import com.miis.horusendoview.type.StandbyNotificationTimeType;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import timber.log.Timber;

public class SharedPreferencesManager {

    private final static String NAME_SETTINGS = "SETTINGS";

    private final static String KEY_SETTINGS_STANDBY_NOTIFICATION_TIME = "STANDBY_NOTIFICATION_TIME";

    private final static String KEY_SETTINGS_MAXIMUM_VIDEO_DURATION = "MAXIMUM_VIDEO_DURATION";

    private final static String KEY_SETTINGS_IMAGE_ROTATE = "IMAGE_ROTATE";

    private final static String KEY_SETTINGS_BOOT_AUTO_DELETE_FILE = "BOOT_AUTO_DELETE_FILE";

    private final static String KEY_SETTINGS_BACKUP_ROOM_FILE_LAST_DATE_TIME = "BACKUP_ROOM_FILE_LAST_DATE_TIME";


    private final static String NAME_USER = "USER";

    private final static String KEY_USER_REMEMBER_ACCOUNT = "REMEMBER_ACCOUNT";

    private final static String KEY_IS_REMEMBER_ACCOUNT = "IS_REMEMBER_ACCOUNT";

    private static SharedPreferencesManager instance;
    private final static Object instanceLock = new Object();

    //@NonNull
    private MyApplication myApplication;

    private SharedPreferencesManager() {

    }

    /**
     * 取得實例
     */
    public static SharedPreferencesManager getInstance() {
        synchronized (instanceLock) {
            if (instance == null) {
                instance = new SharedPreferencesManager();
            }
            return instance;
        }
    }

    /**
     * 設 MyApplication
     */
    public void setMyApplication(@NonNull MyApplication myApplication) {
        this.myApplication = myApplication;
    }

    /**
     * 存 待機通知時間 設定
     */
    public synchronized void saveStandbyNotificationTimeType(@NonNull StandbyNotificationTimeType type) {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_SETTINGS_STANDBY_NOTIFICATION_TIME, type.name()).commit();
    }


    /**
     * 取 待機通知時間 設定
     */
    @Nullable
    public synchronized StandbyNotificationTimeType getStandbyNotificationTimeType() {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        String str = sharedPreferences.getString(KEY_SETTINGS_STANDBY_NOTIFICATION_TIME, "");
        StandbyNotificationTimeType[] typeArr = StandbyNotificationTimeType.values();
        @Nullable StandbyNotificationTimeType type = StandbyNotificationTimeType.NEVER;
        for (StandbyNotificationTimeType t : typeArr) {
            if (TextUtils.equals(t.name(), str)) {
                type = t;
                break;
            }
        }
        return type;
    }

    /**
     * 存 最大錄影時間 設定
     */
    public synchronized void saveMaximumVideoDurationType(@NonNull MaximumVideoDurationType type) {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_SETTINGS_MAXIMUM_VIDEO_DURATION, type.name()).commit();
    }

    /**
     * 取 最大錄影時間 設定
     */
    @Nullable
    public synchronized MaximumVideoDurationType getMaximumVideoDurationType() {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        String str = sharedPreferences.getString(KEY_SETTINGS_MAXIMUM_VIDEO_DURATION, "");
        MaximumVideoDurationType[] typeArr = MaximumVideoDurationType.values();
        @Nullable MaximumVideoDurationType type = MaximumVideoDurationType.MIN_120;
        for (MaximumVideoDurationType t : typeArr) {
            if (TextUtils.equals(t.name() , str)) {
                type = t;
                break;
            }
        }
        return type;
    }

    /**
     * 存 影像旋轉角度 設定
     */
    public synchronized void saveImageRotateType(@NonNull ImageRotateType type) {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_SETTINGS_IMAGE_ROTATE, type.name()).commit();
    }

    /**
     * 取 影像旋轉角度 設定
     */
    @Nullable
    public synchronized ImageRotateType getImageRotateType() {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        String str = sharedPreferences.getString(KEY_SETTINGS_IMAGE_ROTATE, "");
        ImageRotateType[] typeArr = ImageRotateType.values();
        @Nullable ImageRotateType type = null;
        for (ImageRotateType t : typeArr) {
            if (TextUtils.equals(t.name() , str)) {
                type = t;
                break;
            }
        }
        return type;
    }


    /**
     * 存 自動刪除檔案天數 設定
     */
    public synchronized void saveBootAutoDeleteFileType(@NonNull BootAutoDeleteFileType type) {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_SETTINGS_BOOT_AUTO_DELETE_FILE, type.name()).commit();
    }

    /**
     * 取 自動刪除檔案天數 設定
     */
    @NonNull
    public synchronized BootAutoDeleteFileType getBootAutoDeleteFileType() {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        String str = sharedPreferences.getString(KEY_SETTINGS_BOOT_AUTO_DELETE_FILE, "");
        BootAutoDeleteFileType[] typeArr = BootAutoDeleteFileType.values();
        @Nullable BootAutoDeleteFileType type = null;
        for (BootAutoDeleteFileType t : typeArr) {
            if (TextUtils.equals(t.name(), str)) {
                type = t;
                break;
            }
        }
        if (type == null) {
            type = BootAutoDeleteFileType.DAY_30;
        }
        return type;
    }

    /**
     * db 備份最後的時間
     */
    public synchronized void setBackupRoomFileLastDateTime(@NonNull LocalDateTime localDateTime) {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_SETTINGS_BACKUP_ROOM_FILE_LAST_DATE_TIME, localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(Locale.ENGLISH))).commit();
    }

    /**
     * db 備份最後的時間
     */
    @Nullable
    public synchronized LocalDateTime getBackupRoomFileLastDateTime() {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_SETTINGS, Context.MODE_PRIVATE);
        String str = sharedPreferences.getString(KEY_SETTINGS_BACKUP_ROOM_FILE_LAST_DATE_TIME, "");
        try {
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(Locale.ENGLISH));
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }


    /**
     * 存 已登入帳號
     */
    public synchronized void saveRememberAccount(@NonNull String rememberAccount) {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_USER, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_USER_REMEMBER_ACCOUNT, rememberAccount).commit();
    }

    /**
     * 取 已登入帳號
     */
    @NonNull
    public synchronized String getRememberAccount() {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_USER, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_REMEMBER_ACCOUNT, "");
    }

    /**
     * 存 是否存已登入帳號開關
     */
    public synchronized void saveRememberAccountEnable(boolean isEnable) {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_USER, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(KEY_IS_REMEMBER_ACCOUNT, isEnable).commit();
    }

    /**
     * 取 是否存已登入帳號開關
     */
    public synchronized boolean isRememberAccountEnable() {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences(NAME_USER, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_IS_REMEMBER_ACCOUNT, false);
    }
}
