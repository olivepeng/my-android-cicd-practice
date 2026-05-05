package com.miis.horusendoview.manager;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.system.OsConstants;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.dicom.ImageToDicomService;
import com.miis.horusendoview.errorcode.Error;
import com.miis.horusendoview.errorcode.IErrorCode;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import org.dcm4che3.data.Tag;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MyStorageManager {
    private static MyStorageManager instance;

    private static final Object instanceLock = new Object();

    private MyApplication myApplication;

    @Nullable
    private StorageManager storageManager;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    @NonNull
    private List<StorageVolume> storageList = new ArrayList<>();
    private final Object storageListLock = new Object();


    @NonNull
    private List<Listener> listenerList = new ArrayList<>();
    private final Object listenerListLock = new Object();

    public interface Listener {
        void onStorageMounted(@NonNull File storageFile);

        void onStorageUnmounted(@NonNull File storageFile);
    }

    @NonNull
    public static synchronized MyStorageManager getInstance() {
        synchronized(instanceLock) {
            if (instance == null) {
                instance = new MyStorageManager();
            }
            return instance;
        }
    }

    private MyStorageManager() {
        if (scheduledExecutorService instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor s = (ScheduledThreadPoolExecutor) scheduledExecutorService;
            s.setRemoveOnCancelPolicy(true);
        }
    }

    public void init(@NonNull final MyApplication myApplication) {

        this.myApplication = myApplication;

        Object systemService = myApplication.getSystemService(Context.STORAGE_SERVICE);
        if (systemService instanceof StorageManager) {
            storageManager = (StorageManager) systemService;
        }

        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                clearStorage();
                List<StorageVolume> storageVolumeList = new ArrayList<>();
                if (storageManager != null) {
                    storageVolumeList = storageManager.getStorageVolumes();
                }
//                if (storageVolumeList == null) {
//                    storageVolumeList = new ArrayList<>();
//                }

                for (StorageVolume storageVolume : storageVolumeList) {
                    if (storageVolume.isRemovable()) {
                        addStorage(storageVolume);
                    }
                }
            }
        });
    }

    public final synchronized void addStorage(@NonNull File file) {
        synchronized (storageListLock) {

            final StorageManager storageManager = this.storageManager;

            if (TextUtils.equals(file.getAbsolutePath(), Environment.getExternalStorageDirectory().getAbsolutePath())) {
                // 除掉自己的目錄
                return;
            }

            if (!file.isDirectory()) {
                return;
            }

            if (!file.canRead()) {
                return;
            }

            if (!file.canWrite()) {
                return;
            }

            if (storageManager == null) {
                return;
            }

            StorageVolume storageVolume = storageManager.getStorageVolume(file);
            if (storageVolume != null) {
                addStorage(storageVolume);
            }
        }
    }

    public synchronized void addStorage(@NonNull StorageVolume storageVolume) {

        synchronized (storageListLock) {
            if (!storageVolume.isRemovable()) {
                return;
            }

            File storageVolumeDirectory = storageVolume.getDirectory();
            if (storageVolumeDirectory == null) {
                return;
            }

            if (!storageVolumeDirectory.isDirectory()) {
                return;
            }

            if (!storageVolumeDirectory.canWrite()) {
                return;
            }

            if (!storageVolumeDirectory.canRead()) {
                return;
            }

            ArrayList<StorageVolume> list = new ArrayList<>(storageList);


            boolean isAdd = false;
            for (StorageVolume d : list) {
                File d2 = d.getDirectory();
                if (d2 != null && TextUtils.equals(d2.getAbsolutePath() ,storageVolumeDirectory.getAbsolutePath())) {
                    isAdd = true;
                    break;
                }
            }

            if (isAdd) {
                return;
            }

            list.add(storageVolume);
            storageList = list;
        }
    }

    public synchronized void removeStorage(@NonNull File file) {
        synchronized (storageListLock) {
            ArrayList<StorageVolume> list = new ArrayList<>(storageList);

            List<StorageVolume> removeList = list.stream().filter(new Predicate<StorageVolume>() {
                @Override
                public boolean test(StorageVolume storageVolume) {
                    if (storageVolume == null) {
                        return false;
                    }
                    File directory = storageVolume.getDirectory();
                    if (directory != null) {
                        return TextUtils.equals(directory.getAbsolutePath() , file.getAbsolutePath());
                    }
                    return false;
                }
            }).collect(Collectors.toList());

            if (removeList.isEmpty()) {
                return;
            }

            list.removeAll(removeList);

            storageList = list;
        }
    }

    public synchronized void clearStorage() {
        synchronized(storageListLock) {
            this.storageList = new ArrayList<>();
        }
    }

    @NonNull
    public final synchronized List<StorageVolume> getStorageList() {
        synchronized (storageListLock) {
            return new ArrayList(storageList);
        }
    }

    public final synchronized void addListener(@NonNull Listener listener) {
        synchronized (listenerListLock) {
            ArrayList<Listener> list = new ArrayList<>(listenerList);

            boolean isAdd = false;

            for (Listener l : list) {
                if (listener == l) {
                    isAdd = true;
                    break;
                }
            }

            if (isAdd) {
                return;
            }

            list.add(listener);
            listenerList = list;
        }
    }

    public final synchronized void removeListener(@NonNull Listener listener) {

        synchronized (listenerListLock) {
            ArrayList<Listener> list = new ArrayList<>(listenerList);
            list.removeIf(new Predicate<Listener>() {
                @Override
                public boolean test(Listener listener02) {
                    return listener == listener02;
                }
            });
            this.listenerList = list;
        }
    }

    public final synchronized void clearListener() {
        synchronized (listenerListLock) {
            this.listenerList = new ArrayList<>();
        }
    }

    public final synchronized void sendOnStorageMounted(@NonNull File storageFile) {
        synchronized (listenerListLock) {
            final List<Listener> list = this.listenerList;

            for (Listener l : list) {
                l.onStorageMounted(storageFile);
            }
        }
    }

    public final synchronized void sendOnStorageUnmounted(@NonNull File storageFile) {
        synchronized (listenerListLock) {
            final List<Listener> list = this.listenerList;

            for (Listener l : list) {
                l.onStorageUnmounted(storageFile);
            }
        }
    }

    /**
     * 複製單一檔案並傳回一個Flowable。
     * 此Flowable在複製檔案時會發出每秒傳輸速度和預期傳輸時間的更新。
     * 在完成時，Flowable也會傳送一個進度值為100%的ProgressData物件。
     * 呼叫方可以透過訂閱該Flowable來監視檔案複製進度，並在完成時執行某些操作。
     */
    public Observable<ProgressData> copyFileWithProgressFlow(
            @NonNull File sourceFile,
            @NonNull File destFile
    ) {
        return Observable.create(new ObservableOnSubscribe<ProgressData>() {
                    @Override
                    public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<ProgressData> emitter) throws Throwable {
                        final long startTimeMillis = System.currentTimeMillis();

                        final long progressUpdateIntervalMillis = 1000L;
                        final long bufferSize = 8192L; // 缓冲区大小，根据实际情况调整
                        long totalBytesCopied = 0L;

                        if (destFile.exists()) {
                            try {
                                if(!destFile.delete()){
                                    Timber.d("delete file fail: "+destFile.getAbsolutePath());
                                }
                            } catch (Exception e) {
                                Timber.e(e);
                            }
                        }


                        final long totalBytes = sourceFile.length();

                        FileInputStream sourceStream = null;
                        FileOutputStream destStream = null;

                        try {
                            sourceStream = new FileInputStream(sourceFile);
                            destStream = new FileOutputStream(destFile , true);

                            byte[] buffer = new byte[(int) bufferSize];
                            int bytesRead;
                            long lastProgressUpdate = 0L;
                            long lastTimeUpdate = System.currentTimeMillis();

                            while ((bytesRead = sourceStream.read(buffer)) != -1) {
                                if (emitter.isDisposed()) {
                                    if (sourceStream != null) {
                                        try {
                                            sourceStream.close();
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }
                                    }

                                    if (destStream != null) {
                                        try {
                                            destStream.flush();
                                        } catch (IOException e2) {
                                            Timber.e(e2);
                                        }

                                        try {
                                            destStream.getFD().sync();
                                        } catch (IOException e2) {
                                            Timber.e(e2);
                                        }

                                        try {
                                            destStream.close();
                                        } catch (Exception e2) {
                                            Timber.e(e2);
                                        }
                                    }
                                    return;
                                }

//                                FileUtils.writeByteArrayToFile(destFile, buffer, 0, bytesRead, true);
                                destStream.write(buffer, 0, bytesRead);
                                totalBytesCopied += bytesRead;

                                if (emitter.isDisposed()) {
                                    if (sourceStream != null) {
                                        try {
                                            sourceStream.close();
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }
                                    }

                                    if (destStream != null) {
                                        try {
                                            destStream.flush();
                                        } catch (IOException e2) {
                                            Timber.e(e2);
                                        }

                                        try {
                                            destStream.getFD().sync();
                                        } catch (IOException e2) {
                                            Timber.e(e2);
                                        }

                                        try {
                                            destStream.close();
                                        } catch (Exception e2) {
                                            Timber.e(e2);
                                        }
                                    }
                                    return;
                                }

                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastTimeUpdate > progressUpdateIntervalMillis) {
                                    long elapsedTime = currentTime - lastTimeUpdate;
                                    long remainingBytes = totalBytes - totalBytesCopied;
                                    long bytesPerSecond = (long) Math.ceil((totalBytesCopied - lastProgressUpdate) /
                                            (elapsedTime / 1000.0));
                                    long secondsRemaining = (bytesPerSecond > 0) ? remainingBytes / bytesPerSecond : 0L;
                                    int progress = (int) ((totalBytesCopied * 100) / totalBytes);

                                    long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                                    double bytesPerMillis = (double) totalBytesCopied / elapsedMillis;
                                    long remainingMillis = (long) (remainingBytes / bytesPerMillis);

                                    emitter.onNext(new ProgressData(
                                            progress,
                                            bytesPerSecond,
                                            secondsRemaining,
                                            remainingMillis / 1000,
                                            destFile,
                                            totalBytes,
                                            totalBytesCopied
                                    ));

                                    lastProgressUpdate = totalBytesCopied;
                                    lastTimeUpdate = currentTime;

                                    if (emitter.isDisposed()) {
                                        if (sourceStream != null) {
                                            try {
                                                sourceStream.close();
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }
                                        }

                                        if (destStream != null) {
                                            try {
                                                destStream.flush();
                                            } catch (IOException e2) {
                                                Timber.e(e2);
                                            }

                                            try {
                                                destStream.getFD().sync();
                                            } catch (IOException e2) {
                                                Timber.e(e2);
                                            }

                                            try {
                                                destStream.close();
                                            } catch (Exception e2) {
                                                Timber.e(e2);
                                            }
                                        }
                                        return;
                                    }
                                }
                            }
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        } finally {
                            if (sourceStream != null) {
                                try {
                                    sourceStream.close();
                                } catch (Exception e) {
                                    Timber.e(e);
                                }
                            }

                            if (destStream != null) {
                                try {
                                    destStream.flush();
                                } catch (IOException e2) {
                                    Timber.e(e2);
                                }

                                try {
                                    destStream.getFD().sync();
                                } catch (IOException e2) {
                                    Timber.e(e2);
                                }

                                try {
                                    destStream.close();
                                } catch (Exception e2) {
                                    Timber.e(e2);
                                }
                            }
                        }
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * 複製多個檔案並傳回一個Flowable。
     *
     * @param filesToCopy 包含原始檔案和目標檔案的對的清單。 (左為來源文件，右為目標文件)
     * @return 包含所有檔案複製進度更新的Flowable。
     */
    public Observable<ProgressData> copyFilesWithProgressFlow(@NonNull List<@NotNull Pair<@NotNull File, @NotNull File>> filesToCopy) {
        return Observable.create(new ObservableOnSubscribe<ProgressData>() {
                    @Override
                    public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<ProgressData> emitter) throws Throwable {
                        final long startTimeMillis = System.currentTimeMillis();

                        long totalBytes = 0;
                        for (Pair<File, File> filePair : filesToCopy) {
                            totalBytes += filePair.first.length();
                        }

                        if (emitter.isDisposed()) {
                            return;
                        }

                        long totalBytesCopied = 0;

                        long lastProgressUpdate = 0;
                        long lastTimeUpdate = System.currentTimeMillis();

                        for (Pair<File, File> filePair : filesToCopy) {
                            if (emitter.isDisposed()) {
                                return;
                            }
                            File sourceFile = filePair.first;
                            File destFile = filePair.second;

                            if (destFile.exists()) {
                                try {
                                    if(!destFile.delete()){
                                        Timber.d("delete file fail: "+destFile.getAbsolutePath());
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }
                            }

                            try {
                                if (!destFile.getParentFile().exists()) {
                                    destFile.getParentFile().mkdirs();
                                }
                            } catch (Exception e) {
                                Timber.e(e);
                            }

                            long bufferSize = 8192L; // 缓冲区大小，根据实际情况调整
                            FileInputStream sourceStream = null;
                            FileOutputStream destStream = null;

                            try {
                                sourceStream = new FileInputStream(sourceFile);

                                destStream = new FileOutputStream(destFile , true);

                                Timber.d("copyFilesWithProgressFlow sourceStream=" + sourceStream);

                                byte[] buffer = new byte[(int) bufferSize];
                                int bytesRead;

                                while ((bytesRead = sourceStream.read(buffer)) != -1) {
                                    if (emitter.isDisposed()) {
                                        if (sourceStream != null) {
                                            try {
                                                sourceStream.close();
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }
                                        }

                                        if (destStream != null) {
                                            try {
                                                destStream.flush();
                                            } catch (IOException e2) {
                                                Timber.e(e2);
                                            }

                                            try {
                                                destStream.getFD().sync();
                                            } catch (IOException e2) {
                                                Timber.e(e2);
                                            }

                                            try {
                                                destStream.close();
                                            } catch (Exception e2) {
                                                Timber.e(e2);
                                            }
                                        }
                                        return;
                                    }
//                                    FileUtils.writeByteArrayToFile(destFile, buffer, 0, bytesRead, true);
                                    destStream.write(buffer, 0, bytesRead);
//                                    destStream.flush();
                                    totalBytesCopied += bytesRead;

                                    if (emitter.isDisposed()) {
                                        if (sourceStream != null) {
                                            try {
                                                sourceStream.close();
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }
                                        }

                                        if (destStream != null) {
                                            try {
                                                destStream.flush();
                                            } catch (IOException e2) {
                                                Timber.e(e2);
                                            }

                                            try {
                                                destStream.getFD().sync();
                                            } catch (IOException e2) {
                                                Timber.e(e2);
                                            }

                                            try {
                                                destStream.close();
                                            } catch (Exception e2) {
                                                Timber.e(e2);
                                            }
                                        }
                                        return;
                                    }

                                    long currentTime = System.currentTimeMillis();
                                    if (currentTime - lastTimeUpdate > 1000) { // 每秒更新一次传输速度和预计传输时间
                                        final long elapsedTime = currentTime - lastTimeUpdate;
                                        final long remainingBytes = totalBytes - totalBytesCopied;
                                        final long bytesPerSecond = (long) Math.ceil((totalBytesCopied - lastProgressUpdate) /
                                                (elapsedTime / 1000.0));
                                        final long secondsRemaining = (bytesPerSecond > 0) ? remainingBytes / bytesPerSecond : 0L;
                                        final int progress = (int) ((totalBytesCopied * 100) / totalBytes);

                                        final long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                                        final double bytesPerMillis = (double) totalBytesCopied / elapsedMillis;
                                        final long remainingMillis = (long) (remainingBytes / bytesPerMillis);

                                        emitter.onNext(new ProgressData(
                                                progress,
                                                bytesPerSecond,
                                                secondsRemaining,
                                                remainingMillis / 1000,
                                                destFile,
                                                totalBytes,
                                                totalBytesCopied
                                        ));
                                        lastProgressUpdate = totalBytesCopied;
                                        lastTimeUpdate = currentTime;

                                        if (emitter.isDisposed()) {
                                            if (sourceStream != null) {
                                                try {
                                                    sourceStream.close();
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }
                                            }

                                            if (destStream != null) {
                                                try {
                                                    destStream.flush();
                                                } catch (IOException e2) {
                                                    Timber.e(e2);
                                                }

                                                try {
                                                    destStream.getFD().sync();
                                                } catch (IOException e2) {
                                                    Timber.e(e2);
                                                }

                                                try {
                                                    destStream.close();
                                                } catch (Exception e2) {
                                                    Timber.e(e2);
                                                }
                                            }
                                            return;
                                        }
                                    }
                                }

                                if (sourceStream != null) {
                                    try {
                                        sourceStream.close();
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }
                                }

                                if (destStream != null) {
                                    try {
                                        destStream.flush();
                                    } catch (IOException e2) {
                                        Timber.e(e2);
                                    }

                                    try {
                                        destStream.getFD().sync();
                                    } catch (IOException e2) {
                                        Timber.e(e2);
                                    }

                                    try {
                                        destStream.close();
                                    } catch (Exception e2) {
                                        Timber.e(e2);
                                    }
                                }

                            } catch (Exception e) {
                                emitter.onError(e);
                            } finally {
                                if (sourceStream != null) {
                                    try {
                                        sourceStream.close();
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }
                                }

                                if (destStream != null) {
                                    try {
                                        destStream.flush();
                                    } catch (IOException e2) {
                                        Timber.e(e2);
                                    }

                                    try {
                                        destStream.getFD().sync();
                                    } catch (IOException e2) {
                                        Timber.e(e2);
                                    }

                                    try {
                                        destStream.close();
                                    } catch (Exception e2) {
                                        Timber.e(e2);
                                    }
                                }
                            }
                        }

                        emitter.onNext(new ProgressData(
                                100,
                                0,
                                0,
                                0,
                                null,
                                totalBytes,
                                totalBytes
                        ));

                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * 多個照片轉DICOM檔 並傳回一個Flowable。
     *
     * @param filesToCopy 包含原始檔案和目標檔案的對的清單。 (左為來源文件，右為目標文件)
     * @return 包含所有檔案複製進度更新的Flowable。
     */
    public Observable<ProgressData> image2DicomWithProgressFlow(@NonNull List<@NotNull Pair<@NotNull File, @NotNull File>> filesToCopy, @NonNull Map<Integer,String > dicomInfo) {
        return Observable.create(new ObservableOnSubscribe<ProgressData>() {
            @Override
            public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<ProgressData> emitter) throws Throwable {
                if (emitter.isDisposed()) {
                    return;
                }

                long totalBytesCopied = 0;

                for (Pair<File, File> filePair : filesToCopy) {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    File sourceFile = filePair.first;
                    File destFile = filePair.second;

                    if (destFile.exists()) {
                        try {
                            if(!destFile.delete()){
                                Timber.d("delete file fail: "+destFile.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    try {
                        if (!destFile.getParentFile().exists()) {
                            destFile.getParentFile().mkdirs();
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    //Convert image file to DICOM
                    try {
                        final String name = sourceFile.getName();
                        final int index = name.lastIndexOf("-");
                        if(index==(name.length()-8) && index>8) {
                            final String study_time = name.substring(index - 8, index);
                            dicomInfo.put(Tag.StudyTime, study_time);
                        }else {
                            LocalDateTime fileTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(sourceFile.lastModified()), ZoneId.systemDefault()
                            );
                            dicomInfo.put(Tag.StudyTime, fileTime.format(DateTimeFormatter.ofPattern("HH-mm-ss", Locale.ENGLISH)));
                        }
                        convertJpeg2DCM(sourceFile.getAbsolutePath(),destFile.getAbsolutePath(), dicomInfo);
                    } catch (Exception exception) {
                        //exception.printStackTrace();
                        Timber.e("[image2DicomWithProgressFlow]"+exception.getMessage());
                        if(!emitter.isDisposed()) {
                            emitter.onError(exception);
                        }
                    }


                    emitter.onNext(new ProgressData(
                            100,
                            0,
                            0,
                            0,
                            null,
                            ++totalBytesCopied,
                            filesToCopy.size()
                    ));

                }

                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io());
    }

    public static class ProgressData {
        private final int progress;
        private final long bytesPerSecond;
        private final long secondsRemaining;
        private final long remainingTimeSec;
        @Nullable
        private final File file;
        private final long totalBytes;
        private final long totalBytesCopied;

        public ProgressData(int progress, long bytesPerSecond, long secondsRemaining, long remainingTimeSec, @Nullable File file, long totalBytes, long totalBytesCopied) {
            this.progress = progress;
            this.bytesPerSecond = bytesPerSecond;
            this.secondsRemaining = secondsRemaining;
            this.remainingTimeSec = remainingTimeSec;
            this.file = file;
            this.totalBytes = totalBytes;
            this.totalBytesCopied = totalBytesCopied;
        }

        public final int getProgress() {
            return this.progress;
        }

        public final long getBytesPerSecond() {
            return this.bytesPerSecond;
        }

        public final long getSecondsRemaining() {
            return this.secondsRemaining;
        }

        public final long getRemainingTimeSec() {
            return this.remainingTimeSec;
        }


        @Nullable
        public final File getFile() {
            return this.file;
        }

        public final long getTotalBytes() {
            return this.totalBytes;
        }

        public final long getTotalBytesCopied() {
            return this.totalBytesCopied;
        }

        @NonNull
        public String toString() {
            return "ProgressData(progress=" + this.progress + ", bytesPerSecond=" + this.bytesPerSecond + ", secondsRemaining=" + this.secondsRemaining + ", remainingTimeSec=" + this.remainingTimeSec + ", file=" + this.file + ", totalBytes=" + this.totalBytes + ", totalBytesCopied=" + this.totalBytesCopied + ")";
        }
    }


    /**
     * 壓縮檔案並傳回一個Flowable。
     *
     * @param fileMapList 包含原始檔案和目標檔案的對應清單。
     * @param password    壓縮密碼。
     * @return 包含所有檔案壓縮進度更新的Flowable。
     */
    public Observable<ZipProgressData> compressFilesToZipsWithPasswordFlow(
            @NonNull Map<@NotNull File, @NotNull List<@NotNull File>> fileMapList,
            @NonNull String password
    ) {
        return Observable.create(new ObservableOnSubscribe<ZipProgressData>() {
                    @Override
                    public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<ZipProgressData> emitter) throws Throwable {
                        int totalFiles = 0;
                        for (Map.Entry<File, List<File>> fileMap : fileMapList.entrySet()) {
                            totalFiles += fileMap.getValue().size();
                        }
                        Timber.d("compressFilesToZipsWithPasswordFlow 01 totalFiles=" + totalFiles);

                        final double bytesPerFile = 100.0 / totalFiles;
                        double progress = 0.0;
                        ZipFile zipFile = null;
                        try {
                            for (Map.Entry<File, List<File>> fileMap : fileMapList.entrySet()) {
                                Timber.d("compressFilesToZipsWithPasswordFlow 02 fileMap=" + fileMap);
                                if (emitter.isDisposed()) {
                                    return;
                                }
                                File keyFile = fileMap.getKey();

                                if (emitter.isDisposed()) {
                                    return;
                                }

                                try {
                                    if (keyFile.exists()) {
                                        if(!keyFile.delete()){
                                            Timber.d("delete file fail: "+keyFile.getAbsolutePath());
                                        }
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                if (emitter.isDisposed()) {
                                    return;
                                }

                                try {
                                    if (!keyFile.getParentFile().exists()) {
                                        keyFile.getParentFile().mkdirs();
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                if (emitter.isDisposed()) {
                                    return;
                                }

                                if (password != null && !password.trim().isEmpty()) {
                                    zipFile = new ZipFile(keyFile, password.trim().toCharArray());
                                } else {
                                    zipFile = new ZipFile(keyFile);
                                }

                                ZipParameters parameters = new ZipParameters();
                                parameters.setCompressionMethod(CompressionMethod.DEFLATE);
                                parameters.setCompressionLevel(CompressionLevel.NORMAL);
                                if (password != null && !password.trim().isEmpty()) {
                                    parameters.setEncryptFiles(true);

                                    parameters.setEncryptionMethod(EncryptionMethod.AES);//Jerry
                                    parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);//Jerry
                                } else {
                                    parameters.setEncryptFiles(false);
                                }

                                List<File> fileList = fileMap.getValue();
                                for (int index = 0; index < fileList.size(); index++) {
                                    File file = fileList.get(index);
                                    Timber.d("compressFilesToZipsWithPasswordFlow 03 file=" + file);
                                    emitter.onNext(new ZipProgressData(progress, file, zipFile.getFile()));

                                    if (emitter.isDisposed()) {
                                        return;
                                    }
                                    try {
                                        zipFile.addFile(file, parameters);
                                    } catch (Exception e) {
                                        Timber.e(e);
                                        if (e instanceof FileNotFoundException) {
                                            emitter.onError(e);
                                            return;
                                        }
                                    }

                                    Timber.d("compressFilesToZipsWithPasswordFlow 04");
                                    progress += bytesPerFile;
                                    emitter.onNext(new ZipProgressData(progress, file, zipFile.getFile()));

                                    if (emitter.isDisposed()) {
                                        return;
                                    }
                                }

                                try {
                                    zipFile.close();
                                    zipFile=null;
                                } catch (Exception e) {
                                    Timber.e(e);
                                }
                            }

                            if (emitter.isDisposed()) {
                                return;
                            }
                            emitter.onNext(new ZipProgressData(100.0, null, null));
                            emitter.onComplete();

                        } catch (Exception e) {
                            emitter.onError(e);
                            return;
                        }finally {
                            if(zipFile!=null){
                                zipFile.close();
                            }
                        }
                        Timber.d("compressFilesToZipsWithPasswordFlow 05");
                    }
                })
                .subscribeOn(Schedulers.io());
    }


    public Observable<ZipProgressData> compressFilesToZipsCopyWithPasswordFlow(
            @NonNull Map<@NotNull File, @NotNull List<@NotNull File>> fileMapList,
            @NonNull String password
    ) {
        return Observable.create(new ObservableOnSubscribe<ZipProgressData>() {
                    @Override
                    public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<ZipProgressData> emitter) throws Throwable {
                        int totalFiles = 0;
                        for (Map.Entry<File, List<File>> fileMap : fileMapList.entrySet()) {
                            totalFiles += fileMap.getValue().size();
                        }
                        Timber.d("compressFilesToZipsWithPasswordFlow 01 totalFiles=" + totalFiles);

                        final double bytesPerFile = 100.0 / totalFiles;
                        double progressOld = 0.0;
                        double progress = 0.0;
                        ZipFile zipFile = null;
                        try {
                            for (Map.Entry<File, List<File>> fileMap : fileMapList.entrySet()) {
                                Timber.d("compressFilesToZipsWithPasswordFlow 02 fileMap=" + fileMap);
                                if (emitter.isDisposed()) {
                                    return;
                                }
                                File keyFile = fileMap.getKey();

                                if (emitter.isDisposed()) {
                                    return;
                                }

                                try {
                                    if (keyFile.exists()) {
                                        if(!keyFile.delete()){
                                            Timber.w("delete file fail: "+keyFile.getAbsolutePath());
                                        }
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                if (emitter.isDisposed()) {
                                    return;
                                }

                                try {
                                    if (!keyFile.getParentFile().exists()) {
                                        keyFile.getParentFile().mkdirs();
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                if (emitter.isDisposed()) {
                                    return;
                                }


                                File zipTempFile = new File(myApplication.getMainDirPath() + File.separator + MyApplication.ZIP_TEMP_NAME + File.separator + keyFile.getName());
                                try {
                                    if (!zipTempFile.getParentFile().exists()) {
                                        zipTempFile.mkdirs();
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                    emitter.onError(e);
                                    return;
                                }

                                try {
                                    File[] lf = new File[]{};
                                    if (zipTempFile.getParentFile() != null) {
                                        lf = zipTempFile.getParentFile().listFiles();
                                    }
                                    if (lf == null) {
                                        lf = new File[]{};
                                    }
                                    for (File f : lf) {
                                        try {
                                            if(!f.delete()){
                                                Timber.w("delete file fail: "+f.getAbsolutePath());
                                            }
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                try {
                                    if (zipTempFile.exists()) {
                                        if(!zipTempFile.delete()){
                                            Timber.w("delete file fail: "+zipTempFile.getAbsolutePath());
                                        }
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                if (password != null && !password.trim().isEmpty()) {
                                    zipFile = new ZipFile(zipTempFile, password.trim().toCharArray());
                                } else {
                                    zipFile = new ZipFile(zipTempFile);
                                }

                                ZipParameters parameters = new ZipParameters();
                                parameters.setCompressionMethod(CompressionMethod.DEFLATE);
                                parameters.setCompressionLevel(CompressionLevel.NORMAL);
                                if (password != null && !password.trim().isEmpty()) {
                                    parameters.setEncryptFiles(true);

                                    parameters.setEncryptionMethod(EncryptionMethod.AES);//Jerry
                                    parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);//Jerry

                                } else {
                                    parameters.setEncryptFiles(false);
                                }

                                List<File> fileList = fileMap.getValue();
                                for (int index = 0; index < fileList.size(); index++) {
                                    File file = fileList.get(index);
                                    Timber.d("compressFilesToZipsWithPasswordFlow 03 file=" + file);
                                    emitter.onNext(new ZipProgressData(progress, file, zipFile.getFile()));

                                    if (emitter.isDisposed()) {
                                        return;
                                    }
                                    try {
                                        zipFile.addFile(file, parameters);
                                    } catch (Exception e) {
                                        Timber.e(e);
                                        if (e instanceof FileNotFoundException) {
                                            emitter.onError(e);
                                            return;
                                        }
                                    }

                                    Timber.d("compressFilesToZipsWithPasswordFlow 04");
                                    progressOld = progress;
                                    progress += bytesPerFile;
//                                    emitter.onNext(new ZipProgressData(progress, file, zipFile.getFile()));
                                    emitter.onNext(new ZipProgressData(progressOld + ((progress - progressOld) / 2), file, zipFile.getFile()));

                                    if (emitter.isDisposed()) {
                                        return;
                                    }
                                }

                                {

                                    if (emitter.isDisposed()) {
                                        return;
                                    }
                                    File sourceFile = zipFile.getFile();
                                    File destFile = keyFile;

                                    if (destFile.exists()) {
                                        try {
                                            if(!destFile.delete()){
                                                Timber.w("delete file fail: "+destFile.getAbsolutePath());
                                            }
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }
                                    }

                                    try {
                                        if (!destFile.getParentFile().exists()) {
                                            destFile.getParentFile().mkdirs();
                                        }
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }


                                    try {
                                        zipFile.close();
                                        zipFile=null;
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }

                                    Timber.d("compressFilesToZipsWithPasswordFlow 05");

                                    try {
//                                        Files.copy(sourceFile , destFile);

                                        final long startTimeMillis = System.currentTimeMillis();

                                        final long progressUpdateIntervalMillis = 1000L;
                                        final long bufferSize = 8192L; // 缓冲区大小，根据实际情况调整
                                        long totalBytesCopied = 0L;

                                        if (destFile.exists()) {
                                            try {
                                                if(!destFile.delete()){
                                                    Timber.w("delete file fail: "+destFile.getAbsolutePath());
                                                }
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }
                                        }


                                        final long totalBytes = sourceFile.length();

                                        FileInputStream sourceStream = null;
                                        FileOutputStream destStream = null;
                                        try {
                                            sourceStream = new FileInputStream(sourceFile);
                                            destStream = new FileOutputStream(destFile, true);

                                            byte[] buffer = new byte[(int) bufferSize];
                                            int bytesRead;
                                            long lastProgressUpdate = 0L;
                                            long lastTimeUpdate = System.currentTimeMillis();

                                            Timber.d("compressFilesToZipsWithPasswordFlow 09");
                                            while ((bytesRead = sourceStream.read(buffer)) != -1) {
                                                if (emitter.isDisposed()) {
                                                    if (sourceStream != null) {
                                                        try {
                                                            sourceStream.close();
                                                        } catch (Exception e) {
                                                            Timber.e(e);
                                                        }
                                                    }

                                                    if (destStream != null) {
                                                        try {
                                                            destStream.flush();
                                                        } catch (IOException e) {
                                                            Timber.e(e);
                                                        }

                                                        try {
                                                            destStream.getFD().sync();
                                                        } catch (IOException e) {
                                                            Timber.e(e);
                                                        }

                                                        try {
                                                            destStream.close();
                                                        } catch (Exception e) {
                                                            Timber.e(e);
                                                        }
                                                    }
                                                    Timber.d("compressFilesToZipsWithPasswordFlow 10");
                                                    return;
                                                }

                                                Timber.d("compressFilesToZipsWithPasswordFlow 11 bytesRead=" + bytesRead);

//                                                FileUtils.writeByteArrayToFile(destFile, buffer, 0, bytesRead, true);
                                                destStream.write(buffer, 0, bytesRead);
                                                totalBytesCopied += bytesRead;

                                                if (emitter.isDisposed()) {
                                                    if (sourceStream != null) {
                                                        try {
                                                            sourceStream.close();
                                                        } catch (Exception e) {
                                                            Timber.e(e);
                                                        }
                                                    }

                                                    if (destStream != null) {
                                                        try {
                                                            destStream.flush();
                                                        } catch (IOException e) {
                                                            Timber.e(e);
                                                        }

                                                        try {
                                                            destStream.getFD().sync();
                                                        } catch (IOException e) {
                                                            Timber.e(e);
                                                        }

                                                        try {
                                                            destStream.close();
                                                        } catch (Exception e) {
                                                            Timber.e(e);
                                                        }
                                                    }
                                                    return;
                                                }

                                                long currentTime = System.currentTimeMillis();
                                                int progress02 = 0;
                                                if (currentTime - lastTimeUpdate > progressUpdateIntervalMillis) {
                                                    long elapsedTime = currentTime - lastTimeUpdate;
                                                    long remainingBytes = totalBytes - totalBytesCopied;
                                                    long bytesPerSecond = (long) Math.ceil((totalBytesCopied - lastProgressUpdate) /
                                                            (elapsedTime / 1000.0));
                                                    long secondsRemaining = (bytesPerSecond > 0) ? remainingBytes / bytesPerSecond : 0L;
                                                    progress02 = (int) ((totalBytesCopied * 100) / totalBytes);

                                                    long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                                                    double bytesPerMillis = (double) totalBytesCopied / elapsedMillis;
                                                    long remainingMillis = (long) (remainingBytes / bytesPerMillis);

//                                                    emitter.onNext(new ProgressData(
//                                                            progress,
//                                                            bytesPerSecond,
//                                                            secondsRemaining,
//                                                            remainingMillis / 1000,
//                                                            destFile,
//                                                            totalBytes,
//                                                            totalBytesCopied
//                                                    ));

                                                    Timber.d("compressFilesToZipsWithPasswordFlow 12 progress02=" + progress02);

                                                    if (progress02 < 100) {
                                                        emitter.onNext(new ZipProgressData(progressOld + ((progress - progressOld) / 2) + (((progress - progressOld) / 2) * (progress02 / 100.0)), sourceFile, destFile));
                                                    }

                                                    lastProgressUpdate = totalBytesCopied;
                                                    lastTimeUpdate = currentTime;

                                                    if (emitter.isDisposed()) {
                                                        if (sourceStream != null) {
                                                            try {
                                                                sourceStream.close();
                                                            } catch (Exception e) {
                                                                Timber.e(e);
                                                            }
                                                        }

                                                        if (destStream != null) {
                                                            try {
                                                                destStream.flush();
                                                            } catch (IOException e) {
                                                                Timber.e(e);
                                                            }

                                                            try {
                                                                destStream.getFD().sync();
                                                            } catch (IOException e) {
                                                                Timber.e(e);
                                                            }

                                                            try {
                                                                destStream.close();
                                                            } catch (Exception e) {
                                                                Timber.e(e);
                                                            }
                                                        }
                                                        return;
                                                    }
                                                }
                                            }

                                            if (destStream != null) {
                                                try {
                                                    destStream.flush();
                                                } catch (IOException e) {
                                                    Timber.e(e);
                                                }

                                                try {
                                                    destStream.getFD().sync();
                                                } catch (IOException e) {
                                                    Timber.e(e);
                                                    IErrorCode.showErrorCode(myApplication.getMainActivity(), Error.EXTERNAL_USB_MEMORY_FULL);
                                                    emitter.onError(e);
                                                    return;
                                                }

                                                try {
                                                    destStream.close();
                                                } catch (IOException ioException) {
                                                    // make sure the cause is an ErrnoException
                                                    if (ioException.getCause() instanceof android.system.ErrnoException) {
                                                        // if so, we can get to the causing errno
                                                        int errno = ((android.system.ErrnoException) ioException.getCause()).errno;
                                                        // and check for the appropriate value
                                                        if(errno == OsConstants.ENOSPC){
                                                            IErrorCode.showErrorCode(myApplication.getMainActivity(), Error.EXTERNAL_USB_MEMORY_FULL);
                                                            emitter.onError(ioException);
                                                            return;
                                                        }
                                                    }
                                                }catch(Exception e) {
                                                    Timber.e(e);
                                                }
                                            }


//                                            boolean isContentEquals = FileUtils.contentEquals(sourceFile, destFile);
//                                            Timber.d("compressFilesToZipsWithPasswordFlow 08 isContentEquals=" + isContentEquals);
                                            Timber.d("compressFilesToZipsWithPasswordFlow 08 sourceFile.length=" + sourceFile.length());
                                            Timber.d("compressFilesToZipsWithPasswordFlow 08 destFile.length=" + destFile.length());

                                            /*
                                            if (!isContentEquals) {
                                                emitter.onError(new Exception("copy err"));
                                                try {
                                                    sourceStream.close();
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }

                                                if (destStream != null) {
                                                    try {
                                                        destStream.flush();
                                                    } catch (IOException e) {
                                                        Timber.e(e);
                                                    }

                                                    try {
                                                        destStream.getFD().sync();
                                                    } catch (IOException e) {
                                                        Timber.e(e);
                                                    }

                                                    try {
                                                        destStream.close();
                                                    } catch (Exception e) {
                                                        Timber.e(e);
                                                    }
                                                }
                                                return;
                                            }
                                            */

                                        } catch (Exception e) {
                                            emitter.onError(e);
                                            if (sourceStream != null) {
                                                try {
                                                    sourceStream.close();
                                                } catch (Exception e2) {
                                                    Timber.e(e2);
                                                }
                                            }

                                            if (destStream != null) {
                                                try {
                                                    destStream.flush();
                                                } catch (IOException e2) {
                                                    Timber.e(e2);
                                                }

                                                try {
                                                    destStream.getFD().sync();
                                                } catch (IOException e2) {
                                                    Timber.e(e2);
                                                }

                                                try {
                                                    destStream.close();
                                                } catch (Exception e2) {
                                                    Timber.e(e2);
                                                }
                                            }
                                            return;
                                        } finally {
                                            if (sourceStream != null) {
                                                try {
                                                    sourceStream.close();
                                                } catch (Exception e2) {
                                                    Timber.e(e2);
                                                }
                                            }

                                            if (destStream != null) {
                                                try {
                                                    destStream.flush();
                                                } catch (IOException e) {
                                                    Timber.e(e);
                                                }

                                                try {
                                                    destStream.getFD().sync();
                                                } catch (IOException e) {
                                                    Timber.e(e);
                                                }

                                                try {
                                                    destStream.close();
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }
                                            }
                                        }


                                    } catch (Exception e) {
                                        Timber.e(e);
                                        emitter.onError(e);
                                        return;
                                    }

                                    Timber.d("compressFilesToZipsWithPasswordFlow 06");

                                    try {
                                        if(!sourceFile.delete()){
                                            Timber.d("delete file fail: "+sourceFile.getAbsolutePath());
                                        }
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }

                                    emitter.onNext(new ZipProgressData(progress, sourceFile, destFile));
                                }
                            }

                            if (emitter.isDisposed()) {
                                return;
                            }

                            emitter.onNext(new ZipProgressData(100.0, null, null));
                            emitter.onComplete();

                        } catch (Exception e) {
                            Timber.e(e);
                            emitter.onError(e);
                            return;
                        }finally {
                            if(zipFile!=null){
                                zipFile.close();
                            }
                        }
                        Timber.d("compressFilesToZipsWithPasswordFlow 07");
                    }
                })
                .subscribeOn(Schedulers.io());
    }


    public static class ZipProgressData {
        private double progress;
        @Nullable
        private final File fileFrom;
        @Nullable
        private final File fileToZip;

        public ZipProgressData(double progress, @Nullable File fileFrom, @Nullable File fileToZip) {
            this.progress = progress;
            this.fileFrom = fileFrom;
            this.fileToZip = fileToZip;
        }

        public final double getProgress() {
            return this.progress;
        }

        public final void setProgress(double var1) {
            this.progress = var1;
        }

        @Nullable
        public final File getFileFrom() {
            return this.fileFrom;
        }

        @Nullable
        public final File getFileToZip() {
            return this.fileToZip;
        }

        @NonNull
        public String toString() {
            return "ZipProgressData(progress=" + this.progress + ", fileFrom=" + this.fileFrom + ", fileToZip=" + this.fileToZip + ")";
        }
    }

    private synchronized Boolean convertJpeg2DCM (String srcPath , String desPath, Map<Integer,String > dicomInfo) throws Exception {
        //Timber.d("[convertJpeg2DCM]");
        ImageToDicomService imageToDicomService = new ImageToDicomService();
        imageToDicomService.initMetaData("Test Patient","Test Patient0821_1", "F","50","19500101", "20100101", "123", "Study's Description", "123", "20100101", "123", "999", "1","008");
        imageToDicomService.setMetaDate(dicomInfo);
        imageToDicomService.convertJpg2Dcm(srcPath, desPath);
        return true;
    }


}
