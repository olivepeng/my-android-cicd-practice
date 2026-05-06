package com.miis.horusendoview.roomDataBase;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.roomDataBase.imageAdjustmentSetting.ImageAdjustmentSettingTbData;
import com.miis.horusendoview.roomDataBase.imageAdjustmentSetting.ImageAdjustmentSettingTbDataDao;
import com.miis.horusendoview.roomDataBase.procedureFolder.ProcedureFolderTbData;
import com.miis.horusendoview.roomDataBase.procedureFolder.ProcedureFolderTbDataDao;
import com.miis.horusendoview.roomDataBase.typeConverter.UsbSizeConverters;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbDataDao;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

@Database(
        version = 1,
        entities = {
                UserTbData.class,
                ProcedureFolderTbData.class,
                ImageAdjustmentSettingTbData.class
        },
//        autoMigrations = {
//                @AutoMigration(from = 1, to = 2)
//        },
        exportSchema = true
)
@TypeConverters(UsbSizeConverters.class)
public abstract class MyRoomDatabase extends RoomDatabase {

    public static final String DBNAME = "room.db";

    public static final String BACKUP_ROOM_DIR_NAME = "roomBackup";

    public static final String BACKUP_ROOM_FILE_NAME = "Backup";

    public static final String BACKUP_ROOM_FILE_NAME_DATE_TIME_FORMATTER = "yyyy-MM-dd_HH_mm_ss_SSS";

    @Nullable
    private static MyRoomDatabase instance;
    private static final Object instanceLock = new Object();

    @Nullable
    private MyApplication myApplication;

    public abstract UserTbDataDao userTbDataDao();

    public abstract ProcedureFolderTbDataDao procedureFolderTbDataDao();

    public abstract ImageAdjustmentSettingTbDataDao imageAdjustmentSettingTbDataDao();

    public MyRoomDatabase() {
    }


    @NonNull
    public static synchronized MyRoomDatabase getDatabase(@NonNull Context context) {
        synchronized (instanceLock) {
            if (instance == null || !instance.isOpen()) {
                Context appContext = context.getApplicationContext();
                boolean isRobolectric = "robolectric".equals(android.os.Build.FINGERPRINT);

                Builder<MyRoomDatabase> databaseBuilder;

                if (isRobolectric) {
                    // 實務做法：測試環境直接使用「記憶體資料庫」
                    // 優點：不用處理檔案路徑、不用 SQLCipher、跑起來極快
                    databaseBuilder = Room.inMemoryDatabaseBuilder(appContext, MyRoomDatabase.class);
                } else {

                    MyApplication myApplication = (MyApplication) context.getApplicationContext();

                    File dbFile = new File(myApplication.getMainDirPath() + File.separator + DBNAME);

                    if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
                        try {
                            dbFile.getParentFile().mkdirs();
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    databaseBuilder = Room.databaseBuilder(appContext, MyRoomDatabase.class, dbFile.getAbsolutePath());

                    // 只有正式環境才掛載加密工廠 (SupportFactory)
                    // 這行是造成 UnsatisfiedLinkError 的元兇，在測試環境必須避開
                    databaseBuilder.openHelperFactory(new SupportFactory(SQLiteDatabase.getBytes("111".toCharArray())));
                }

                databaseBuilder
                        .addCallback(new Callback() {
                            @Override
                            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                super.onCreate(db);
                                Timber.d("Callback onCreate Version=%s", db.getVersion());
                            }

                            @Override
                            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                super.onOpen(db);
                                Timber.d("Callback onOpen Version=%s", db.getVersion());
                            }

                            @Override
                            public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                super.onDestructiveMigration(db);
                                Timber.d("Callback onDestructiveMigration Version=%s", db.getVersion());
                            }
                        })
                        .setJournalMode(JournalMode.TRUNCATE)
                        .allowMainThreadQueries()
                        .enableMultiInstanceInvalidation()
                        .fallbackToDestructiveMigration();

                instance = databaseBuilder.build();
				// 安全轉型處理 MyApplication
                if (appContext instanceof MyApplication) {
                    instance.myApplication = (MyApplication) appContext;
                }    //            Timber.d("getDatabase DBPath=" + instance.getOpenHelper().getWritableDatabase().getPath());
            }

            Timber.d("getDatabase isOpen=%s", instance.isOpen());
            return instance;
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @WorkerThread
    public void backupRoomFile() {
        synchronized (instanceLock) {
            Timber.d("backupRoomFile 01");
            final MyApplication myApplication = MyRoomDatabase.this.myApplication;
            if (myApplication == null) {
                return;
            }

            @Nullable LocalDateTime lastBackupDateTime = SharedPreferencesManager.getInstance().getBackupRoomFileLastDateTime();
            Timber.d("backupRoomFile lastBackupDateTime=" + lastBackupDateTime);


            File backupRoomDir = new File(myApplication.getMainDirPath() + File.separator + BACKUP_ROOM_DIR_NAME);

            try {
                if (!backupRoomDir.exists()) {
                    backupRoomDir.mkdirs();
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            File roomFile = new File(myApplication.getMainDirPath() + File.separator + DBNAME);


            LocalDateTime roomFileLastLocalDateTime = null;
            try {
                BasicFileAttributes attributes = Files.readAttributes(roomFile.toPath(), BasicFileAttributes.class);

                FileTime lastModifiedTime = attributes.lastModifiedTime();
                roomFileLastLocalDateTime = lastModifiedTime.toInstant().atZone(ZoneOffset.systemDefault()).toLocalDateTime().withNano(0);
            } catch (Exception e) {
                Timber.e(e);
            }

            Timber.d("backupRoomFile roomFileLastLocalDateTime=" + roomFileLastLocalDateTime);

            if (roomFileLastLocalDateTime != null && lastBackupDateTime != null) {
                if (roomFileLastLocalDateTime.isEqual(lastBackupDateTime)) {
                    Timber.d("backupRoomFile 03");
                    return;
                }
            }

            close();

            File backupRoomFile = new File(backupRoomDir, BACKUP_ROOM_FILE_NAME + LocalDateTime.now().format(DateTimeFormatter.ofPattern(BACKUP_ROOM_FILE_NAME_DATE_TIME_FORMATTER, Locale.ENGLISH).withLocale(Locale.ENGLISH)));
            try {
                if (roomFile.isFile() && roomFile.exists()) {
                    FileUtils.copyFile(roomFile, backupRoomFile);
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            MyRoomDatabase.getDatabase(myApplication);

            try {
                BasicFileAttributes attributes = Files.readAttributes(roomFile.toPath(), BasicFileAttributes.class);

                FileTime lastModifiedTime = attributes.lastModifiedTime();
                SharedPreferencesManager.getInstance().setBackupRoomFileLastDateTime(lastModifiedTime.toInstant().atZone(ZoneOffset.systemDefault()).toLocalDateTime().withNano(0));
            } catch (Exception e) {
                Timber.e(e);
            }

            File[] fileArr = new File[0];
            try {
                fileArr = backupRoomDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().contains(BACKUP_ROOM_FILE_NAME);
                    }
                });
            } catch (Exception e) {
                Timber.e(e);
            }

            List<File> fileList = new ArrayList<>();
            if (fileArr != null) {
                fileList = Arrays.asList(fileArr);
            }

            if (fileList.size() > 10) {
                int index = fileList.size() - 11;
                fileList.sort(new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {
                        LocalDateTime dateTime1 = null;
                        try {
                            dateTime1 = LocalDateTime.parse(file1.getName().replaceAll(BACKUP_ROOM_FILE_NAME, ""), DateTimeFormatter.ofPattern(BACKUP_ROOM_FILE_NAME_DATE_TIME_FORMATTER, Locale.ENGLISH).withLocale(Locale.ENGLISH));
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                        LocalDateTime dateTime2 = null;
                        try {
                            dateTime2 = LocalDateTime.parse(file2.getName().replaceAll(BACKUP_ROOM_FILE_NAME, ""), DateTimeFormatter.ofPattern(BACKUP_ROOM_FILE_NAME_DATE_TIME_FORMATTER, Locale.ENGLISH).withLocale(Locale.ENGLISH));
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                        if (dateTime1 != null && dateTime2 != null) {
                            return dateTime1.compareTo(dateTime2);
                        }
                        return 0;
                    }
                });

                for (int i = 0; i < fileList.size(); i++) {
                    if (i >= index) {
                        break;
                    }

                    File f = null;
                    try {
                        f = fileList.get(i);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    try {
                        if (f != null) {
                            if(!f.delete()){
                                Timber.w("delete file fail: "+f.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }

            Timber.d("backupRoomFile 02");
        }
    }

    @UiThread
    public void restoreRoom(@NonNull File restoreRoomFile) {
        synchronized (instanceLock) {
            Timber.d("restoreRoom 01 restoreRoomFile=" + restoreRoomFile);
            try {
                if (!restoreRoomFile.exists() || !restoreRoomFile.isFile()) {
                    return;
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            close();
            final MyApplication myApplication = MyRoomDatabase.this.myApplication;
            if (myApplication == null) {
                return;
            }

            MainActivity mainActivity = myApplication.getMainActivity();

            File roomFile = new File(myApplication.getMainDirPath() + File.separator + DBNAME);
            try {
                if(!roomFile.delete()){
                    Timber.w("delete file fail: "+roomFile.getAbsolutePath());
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            try {
                FileUtils.copyFile(restoreRoomFile, roomFile);
                if (mainActivity != null) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainActivity, R.string.restore_data_success, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                Timber.e(e);
                if (mainActivity != null) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainActivity, R.string.restore_data_fail, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            MyRoomDatabase.getDatabase(myApplication);
            Timber.d("restoreRoom 02");
        }
    }

    @WorkerThread
    @NonNull
    public static List<File> getRoomBackupFileList(@NonNull MyApplication myApplication) {

        File backupRoomDir = new File(myApplication.getMainDirPath() + File.separator + BACKUP_ROOM_DIR_NAME);
        if (backupRoomDir.exists()) {
            File[] fileArr = new File[0];
            try {
                fileArr = backupRoomDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().contains(BACKUP_ROOM_FILE_NAME);
                    }
                });
            } catch (Exception e) {
                Timber.e(e);
            }

            List<File> fileList = new ArrayList<>();
            if (fileArr != null) {
                fileList = Arrays.asList(fileArr);
            }

            fileList.sort(new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    LocalDateTime dateTime1 = null;
                    try {
                        dateTime1 = LocalDateTime.parse(file1.getName().replaceAll(BACKUP_ROOM_FILE_NAME, ""), DateTimeFormatter.ofPattern(BACKUP_ROOM_FILE_NAME_DATE_TIME_FORMATTER, Locale.ENGLISH).withLocale(Locale.ENGLISH));
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    LocalDateTime dateTime2 = null;
                    try {
                        dateTime2 = LocalDateTime.parse(file2.getName().replaceAll(BACKUP_ROOM_FILE_NAME, ""), DateTimeFormatter.ofPattern(BACKUP_ROOM_FILE_NAME_DATE_TIME_FORMATTER, Locale.ENGLISH).withLocale(Locale.ENGLISH));
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (dateTime1 != null && dateTime2 != null) {
                        return dateTime1.compareTo(dateTime2);
                    }
                    return 0;
                }
            });

            return fileList;
        } else {
            return new ArrayList<>();
        }
    }
}
