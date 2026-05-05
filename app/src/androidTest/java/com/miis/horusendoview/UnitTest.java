package com.miis.horusendoview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Environment;
import android.os.storage.StorageVolume;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.fragment.CameraFragment;
import com.miis.horusendoview.fragment.SettingsFragment;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.manager.MyStorageManager;
import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.manager.SystemPropertiesUnit;
import com.miis.horusendoview.type.BootAutoDeleteFileType;
import com.miis.horusendoview.type.MaximumVideoDurationType;
import com.miis.horusendoview.type.StandbyNotificationTimeType;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;
import timber.log.Timber;

public class UnitTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mainActivityRule
            = new ActivityScenarioRule<>(MainActivity.class);


    @Test
    //T4U5-1
    public void testShowError1000() throws Exception {
        //不要充電

        Thread.sleep(1 * 1000);

        setShowError1kForTesting(-1);
        waitForView(withText("Cancel"), isDisplayed()).perform(click());
        onView(ViewMatchers.withText("Error #1000")).check(matches(isDisplayed()));

        setShowError1kForTesting(50);
        Thread.sleep(1 * 1000);
        onView(ViewMatchers.withText("Error #1000")).check(ViewAssertions.doesNotExist());

        setShowError1kForTesting(101);
        Thread.sleep(1 * 1000);
        onView(ViewMatchers.withText("Error #1000")).check(ViewAssertions.doesNotExist());

        setShowError1kForTesting(Integer.MIN_VALUE);
    }

    private void setShowError1kForTesting(int value) {
        mainActivityRule.getScenario().onActivity(activity -> {
            activity.setTestShowError1000(value);
        });
    }

    @Test
    //T4U5-2
    public void testShowError2000() throws Exception {
        waitForView(withId(R.id.cameraView), isDisplayed());

        setShowError2kForTesting(true);
        waitForView(withId(R.id.cameraView), isDisplayed());
        Thread.sleep(12 * 1000);
        waitForView(withText("Error #2000"), isDisplayed());
        setShowError2kForTesting(false);
        waitForView(withText("OK"), isDisplayed()).perform(click());
        Thread.sleep(12 * 1000);
        onView(ViewMatchers.withId(R.id.tvMsg)).check(ViewAssertions.doesNotExist());
    }

    private void setShowError2kForTesting(boolean b) {
        mainActivityRule.getScenario().onActivity(activity -> {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.container);
            assertNotNull(fragment);
            if (fragment instanceof CameraFragment) {
                CameraFragment myFragment = (CameraFragment) fragment;
                myFragment.setTestShowError2000(b);
            }
        });
    }

    @Test
    //T4U5-3
    public void testShowError3000() throws Exception {
        //USB stick (remaining capacity is less than 20M)

        //設置文件路徑和名稱
        File file = new File(Environment.getExternalStoragePublicDirectory("UnitTest"),
                "testShowError3000_fake_video.mp4");

        long targetSizeInMB = 20; // 產生未壓縮約 800MB，zip 後會逼近 500MB
        long targetSizeBytes = targetSizeInMB * 1024 * 1024;

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
            Random random = new Random();

            long written = 0;
            while (written < targetSizeBytes) {
                random.nextBytes(buffer); // 隨機資料（不容易被壓縮）
                bos.write(buffer);
                written += buffer.length;
//                System.out.printf("Progress: %.2f%%\r", (written * 100.0 / targetSizeBytes));
            }
        }
       Timber.d("[testShowError3000] " + file.getName() + " size= " + file.length() / (1024 * 1024) + " MB");

        @NonNull final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
        assertFalse("no USB stick", storageList.isEmpty());
        final StorageVolume storage = storageList.get(0);
        final File exportDirectory = storage.getDirectory();
        if (exportDirectory == null) {
            return;
        }

        // 1. 安排 (Arrange)
        Map<File, List<File>> fileMap = new HashMap<>();
        File outputZip = new File(exportDirectory.getAbsolutePath() + File.separator + MyApplication.FOLDER_NAME + File.separator + "testShowError3000.zip");
        fileMap.put(outputZip, Arrays.asList(file));


        // 2. 執行 (Act)
        CountDownLatch latch = new CountDownLatch(1); // 用來等 onError 或 onC
        final Disposable[] basicExportDisposable = new Disposable[1];
        MyStorageManager.getInstance().compressFilesToZipsCopyWithPasswordFlow(fileMap, "ab123456").subscribe(
                new Observer<MyStorageManager.ZipProgressData>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        basicExportDisposable[0] =d;
                    }

                    @Override
                    public void onNext(MyStorageManager.@io.reactivex.rxjava3.annotations.NonNull ZipProgressData zipProgressData) {

                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        basicExportDisposable[0]=null;
                        Timber.e("壓縮失敗：" + e.getMessage());
                        try {
                            Thread.sleep(1000); // 嘗試等待 UI 更新
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        basicExportDisposable[0]=null;
                        Timber.d("壓縮成功");
                        latch.countDown();
                    }
                }
        );

        // 等待壓縮結束（最多等 30 秒）
        assertTrue("壓縮過程 timeout", latch.await(3*60, TimeUnit.SECONDS));

        // 執行 UI 驗證
        waitForView(allOf(withId(R.id.tvMsg),withText("Error #3000")), isDisplayed());

        file.delete();
    }

    @Test
    //T4U5-4
    public void testShowError4000() throws Exception {
        setShowError4kForTesting("85000");
        onView(isRoot()).perform(waitFor(1500)); // 等畫面穩定（取代 sleep）
        onView(ViewMatchers.withText("Error #4000")).check(ViewAssertions.doesNotExist());

        setShowError4kForTesting("85100");
        onView(isRoot()).perform(waitFor(1000));
        onView(ViewMatchers.withText("Error #4000")).check(matches(isDisplayed()));

        setShowError4kForTesting(null);
    }

    private void setShowError4kForTesting(String value) {
        mainActivityRule.getScenario().onActivity(activity -> {
            final SettingsFragment settingsFragment = activity.getSettingsFragment();
            settingsFragment.setTestShowError4000(value);

        });
    }

    @Test
    //T4U5-5
    public void testShowError5000() throws Exception {
        setShowError5kForTesting(31);
        Thread.sleep(1 * 1000);
        onView(ViewMatchers.withText("Error #5000")).check(ViewAssertions.doesNotExist());

        setShowError5kForTesting(29);
        Thread.sleep(1 * 1000);
        onView(ViewMatchers.withText("Error #5000")).check(matches(isDisplayed()));

        setShowError5kForTesting(Integer.MIN_VALUE);
    }

    private void setShowError5kForTesting(int value) {
        mainActivityRule.getScenario().onActivity(activity -> {
            activity.setTestShowError5000(value);
        });
    }

    @Test
    //T4U5-6
    public void testShowError6000() throws Exception {
        //insert 至少一隻內視鏡

        //CH1
        String prop_key = "persist.horusendoview.ch1";
        String temp = SystemPropertiesUnit.getSystemProperty(prop_key);
        SystemPropertiesUnit.setSystemProperty(prop_key, "9000");
        mainActivityRule.getScenario().onActivity(activity -> {
            final CameraFragment cameraFragment = activity.getCameraFragment();
            final List<UsbDevice> deviceList = cameraFragment.getICameraHelper().getDeviceList();
            if (deviceList != null && deviceList.size() > 0) {
                cameraFragment.iCameraHelperStateCallback.onAttach(deviceList.get(0));
            }
        });

        Thread.sleep(1 * 1000);
        onView(ViewMatchers.withText("Error #6000")).check(matches(isDisplayed()));
        SystemPropertiesUnit.setSystemProperty(prop_key, temp);

        //CH2
        prop_key = "persist.horusendoview.ch2";
        temp = SystemPropertiesUnit.getSystemProperty(prop_key);
        SystemPropertiesUnit.setSystemProperty(prop_key, "9000");
        mainActivityRule.getScenario().onActivity(activity -> {
            final CameraFragment cameraFragment = activity.getCameraFragment();
            final List<UsbDevice> deviceList = cameraFragment.getICameraHelper().getDeviceList();
            if (deviceList != null && deviceList.size() > 0) {
                cameraFragment.iCameraHelperStateCallback.onAttach(deviceList.get(0));
            }
        });

        Thread.sleep(1 * 1000);
        onView(ViewMatchers.withText("Error #6000")).check(matches(isDisplayed()));
        SystemPropertiesUnit.setSystemProperty(prop_key, temp);

    }


    @Test
    //T4U6
    public void UnitTestCopyFiles() throws Exception {
        final int Number_of_tests = 3;

        for (int i = 0; i < Number_of_tests; i++) {
//            //實體USB和影像檔
//            Map<File, List<File>> fileMap = new HashMap<>();
//            @NonNull File mainDirFile = new File(Environment.getExternalStoragePublicDirectory(FOLDER_NAME).getAbsolutePath());
//            final File[] folders = mainDirFile.listFiles();
//            File srcFile = null;
//            for (File folder : folders) {
//                if (folder.isDirectory() &&
//                        !folder.getName().equals(MyRoomDatabase.BACKUP_ROOM_DIR_NAME)) {
//                    final File[] files = folder.listFiles();
//                    srcFile = files[0];
//                    break;
//                } else {
//                    continue;
//                }
//            }
//            assertFalse("no file", srcFile == null);
//
//            @NonNull final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
//            assertFalse("no USB stick", storageList.isEmpty());
//            final StorageVolume storage = storageList.get(0);
//            final File exportDirectory = storage.getDirectory();
//            if (exportDirectory == null) {
//                return;
//            }
//
//            File outputZip = null;
//            @Nullable final File parentFile = srcFile.getParentFile();
//            String parentFileName = parentFile != null ? parentFile.getName() : "";
//            final int i1 = parentFileName.indexOf("_");
//            final int i2 = parentFileName.lastIndexOf("_");
//            final ArrayList<@NotNull File> fileList = new ArrayList<>();
//            if (0 < i1 && i1 < parentFileName.length() &&
//                    0 < i2 && i2 < parentFileName.length() &&
//                    i1 < i2
//            ) {
//                final String dirName = parentFileName.substring(i1 + 1, i2);
//                String name = srcFile.getName();
//                outputZip = new File(exportDirectory.getAbsolutePath() + File.separator +
//                        MyApplication.FOLDER_NAME + File.separator + dirName + ".zip");
//                Timber.d("[UnitTestCopyFiles]targetFile=" + outputZip.getPath());
//                fileList.add(srcFile);
//                fileMap.put(outputZip, fileList);
//            }

            Context context = ApplicationProvider.getApplicationContext();

            File outputZip = new File(context.getCacheDir(), "test_output.zip");
            File inputFile1 = new File(context.getCacheDir(), "test_file1.txt");
            File inputFile2 = new File(context.getCacheDir(), "test_file2.txt");

            // 寫入測試檔案內容
            FileUtils.writeStringToFile(inputFile1, "Hello", Charset.defaultCharset());
            FileUtils.writeStringToFile(inputFile2, "World", Charset.defaultCharset());

            Map<File, List<File>> fileMap = new HashMap<>();
            fileMap.put(outputZip, Arrays.asList(inputFile1, inputFile2));

            TestObserver<MyStorageManager.ZipProgressData> observer = MyStorageManager.getInstance()
                    .compressFilesToZipsCopyWithPasswordFlow(fileMap, "123456")
                    .test();

            // 等待結束
            //observer.awaitTerminalEvent(10, TimeUnit.SECONDS);
            Thread.sleep(10 * 1000);

            // 驗證完成
            observer.assertComplete();
            observer.assertNoErrors();

            assertTrue(outputZip.exists());
            assertTrue(outputZip.length() > 0);

            //clean up
            FileUtils.deleteQuietly(inputFile1);
            FileUtils.deleteQuietly(inputFile2);
            FileUtils.deleteQuietly(outputZip);

        }
    }


    @Test
    //T4U7-1
    public void UnitTestAddLog() throws Exception {
        final int Number_of_tests = 3;

        for (int i = 0; i < Number_of_tests; i++) {

            // 1. 寫一段 log
            String testMsg = "This is a test log with value: " + i;
            long nowTimestamp = System.currentTimeMillis(); //記錄現在時間（可用來比對 log 實際時間）
            Timber.d(testMsg);
            Timber.d("[UnitTestAddLog]" + new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date(nowTimestamp)));

            // 2. 等待 log 被寫入磁碟
            Thread.sleep(1000);

            // 3. 找出最新的 log 檔
            File newestFile = findNewestFile(LogQueue.folder); // 你自己的方法
            Timber.d("[UnitTestAddLog] newestFile.getName() = %s", newestFile.getName());

            // 4. 從最後一行開始向前讀，找有沒有該 log
            boolean found = false;
            try (RandomAccessFile raf = new RandomAccessFile(newestFile, "r")) {
                long length = raf.length();
                long pos = length - 1;
                StringBuilder line = new StringBuilder();

                while (pos >= 0) {
                    raf.seek(pos);
                    char c = (char) raf.read();
                    if (c == '\n') {
                        String thisLine = line.reverse().toString().trim();
                        line.setLength(0); // reset
                        if (thisLine.contains(testMsg)) {
                            Timber.d("[UnitTestAddLog] 找到對應 log 行: %s", thisLine);
                            found = true;

                            //比對時間戳是否與 nowTimestamp 相差 < 10 豪秒
                            // log line 範例：04-15 04:02:56.529  3178  3205 D UnitTest: This is a test log with value: 123
                            Pattern logTimePattern = Pattern.compile("^(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
                            java.util.regex.Matcher matcher = logTimePattern.matcher(thisLine);

                            if (matcher.find()) {
                                String logTimeStr = matcher.group(1); // 04-15 04:02:56.529
                                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                                String fullLogTimeStr = currentYear + "-" + logTimeStr;

                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getDefault());
                                Date logDate = sdf.parse(fullLogTimeStr);

                                if (logDate != null) {
                                    long logTimestamp = logDate.getTime();
                                    long diff = Math.abs(logTimestamp - nowTimestamp);
                                    Timber.d("[UnitTestAddLog] 時間差距 %d 毫秒", diff);

                                    if (diff > 10) {
                                        throw new AssertionError("❌ 時間戳誤差過大：差值 " + diff + " 毫秒");
                                    } else {
                                        Timber.d("✅ 時間戳正確");
                                    }
                                }
                            }


                            break;
                        }
                    } else {
                        line.append(c);
                    }
                    pos--;
                }
            } catch (IOException e) {
                Timber.e("[UnitTestAddLog] %s", e.getMessage());
            }

            if (!found) {
                throw new AssertionError("❌ 沒有找到 log 訊息：" + testMsg);
            } else {
                Timber.d("✅ 測試 log 存在於 log 檔案中");
            }

            Thread.sleep(1 * 1000);

        }
    }

    private File findNewestFile(File folder){
        final File[] files = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().lastIndexOf(".sys") > 0;
            }
        });
        File newestFile =  null;
        long newestLastModified=Long.MIN_VALUE;

        for (File file:files) {
            if(file.lastModified() > newestLastModified){
                newestFile=file;
                newestLastModified=file.lastModified();
            }
        }
        return newestFile;
    }

    @Test
    //T4U7-2
    public void UnitTestRemoveLogFiles() throws Exception {
        final int Number_of_tests = 3;

        for (int i = 0; i < Number_of_tests; i++) {
            //1.	get the log all size
            File folder = LogQueue.folder;
            final File[] files = folder.listFiles();
            long size = 0;
            for (File file : files) {
                size += file.length();
            }
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            Timber.d("[UnitTestRemoveLogFiles] size=" + size);

            //2.	Delete the log
            LogQueue.getInstance().removeOldFilesWhenOverSize(size - files[files.length - 1].length() - files[files.length - 2].length());

            Timber.d("[UnitTestRemoveLogFiles]" + files.length + ", folder.listFiles().length=" + folder.listFiles().length);
            assertTrue(files.length > folder.listFiles().length);

            Thread.sleep(1 * 1000);


        }
    }

    @Test
    //T4U8-1
    //T4U8-2
    public void SetGetSystemPropertyTest() throws Exception {
        String prop_key = SystemPropertiesUnit.PROPERTY_KEY_SERIALNUMBER;
        SystemPropertiesUnit.setSystemProperty(prop_key, "SN123456789");
        String result = SystemPropertiesUnit.getSystemProperty(prop_key);
        assertEquals("Get the value of Serial number is incorrect.", "SN123456789", result);

        prop_key = "persist.horusendoview.ch1";
        SystemPropertiesUnit.setSystemProperty(prop_key, "1000");
        result = SystemPropertiesUnit.getSystemProperty(prop_key);
        assertEquals("Get the value of CH1 H-Cable times is incorrect.", "1000", result);

        prop_key = "persist.horusendoview.ch2";
        SystemPropertiesUnit.setSystemProperty(prop_key, "1000");
        result = SystemPropertiesUnit.getSystemProperty(prop_key);
        assertEquals("Get the value of CH2 H-Cable times is incorrect.", "1000", result);
    }

    @Test
    //T4U9
    public void UnitStandbyNotificationTimeServiceTimerTest() throws Exception {
        //沒有接內視鏡

        SharedPreferencesManager.getInstance()
                .saveStandbyNotificationTimeType(StandbyNotificationTimeType.MIN_10);
        mainActivityRule.getScenario().onActivity(activity -> {
            activity.restartStandbyNotificationTimeServiceTimer();
        });
        Thread.sleep(9 * 60 * 1000 + 100);
        waitForView(withSubstring("The system will be in sleep mode after"), isDisplayed());
        // Screenshot file with timestamp
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String filename = "T4U9-UnitStandbyNotificationTimeServiceTimerTest_" + timestamp + ".png";
        File path = new File(Environment.getExternalStorageDirectory(), "UnitTest");
        if (!path.exists()) path.mkdirs();
        File file = new File(path, filename);
        Thread.sleep(1000);
        device.takeScreenshot(file);
    }

    @Test
    //T4U10-1
    //T4U10-2
    public void SetGetStandbyNotificationTimeTest() throws Exception {
        for (StandbyNotificationTimeType type : StandbyNotificationTimeType.values()) {
            SharedPreferencesManager.getInstance()
                    .saveStandbyNotificationTimeType(type);
            final StandbyNotificationTimeType standbyNotificationTimeType = SharedPreferencesManager.getInstance().getStandbyNotificationTimeType();
            Assert.assertEquals("maximumVideoDurationType is incorrect", type, standbyNotificationTimeType);
        }
    }

    @Test
    //T4U10-3
    //T4U10-4
    public void SetGetMaximumVideoDurationTest() throws Exception {
        for (MaximumVideoDurationType type : MaximumVideoDurationType.values()) {
            SharedPreferencesManager.getInstance()
                    .saveMaximumVideoDurationType(type);
            final MaximumVideoDurationType maximumVideoDurationType = SharedPreferencesManager.getInstance().getMaximumVideoDurationType();
            Assert.assertEquals("maximumVideoDurationType is incorrect", type, maximumVideoDurationType);
        }
    }

    @Test
    //T4U10-5
    //T4U10-6
    public void SetGetAutomaticallyDeleteFilesTest() throws Exception {
        for (BootAutoDeleteFileType type : BootAutoDeleteFileType.values()) {
            SharedPreferencesManager.getInstance()
                    .saveBootAutoDeleteFileType(type);
            final BootAutoDeleteFileType bootAutoDeleteFileType = SharedPreferencesManager.getInstance().getBootAutoDeleteFileType();
            Assert.assertEquals("bootAutoDeleteFileType is incorrect", type, bootAutoDeleteFileType);
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------
    public static void removeAllFiles() throws Exception {
        waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());
        if (isViewVisible(withText("Select all"))) {
            waitForView(withText("Select all"), isEnabled()).perform(click());
            waitForView(withId(R.id.selectAllText), withText("Unselect all"));
            waitForView(withText("Delete"), isEnabled()).perform(click());
            waitForView(withText("Yes"), isEnabled()).perform(click());
            Thread.sleep(1000);
        }
        waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));
        waitForView(withId(R.id.ibLiveView), isEnabled()).perform(click());
    }

    public static boolean isViewVisible(Matcher<View> view) {
        boolean isVisible;
        try {
            onView(view)
                    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            isVisible = true;
        } catch (NoMatchingViewException | AssertionFailedError e) {
            isVisible = false;
        }
        return isVisible;
    }

    public static boolean isViewSelected(Matcher<View> view) {
        boolean isVisible;
        try {
            onView(view)
                    .check(matches(isSelected()));
            isVisible = true;
        } catch (NoMatchingViewException | AssertionFailedError e) {
            isVisible = false;
        }
        return isVisible;
    }

    public static void checkSeekBarValue(Matcher<View> view, int expected_value) {
        onView(view).check((v, noViewFoundException) -> {
            SeekBar seekBar = (SeekBar) v;
            assertEquals("SeekBar value is incorrect", expected_value, seekBar.getProgress());
        });
    }

    public static int random(int max, int min) {
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }
        return (int) (Math.random() * (max - min + 1) + min);
    }

    public static void setChannel(int channel) throws Exception {
        switch (channel) {
            case 2:
                if (isViewVisible(withText("Ch1"))) {
                    waitForView(withText("Ch1"), isEnabled()).perform(click());
                }
                Thread.sleep(1 * 1000);
                onView(ViewMatchers.withText("Ch2")).check(matches(isEnabled()));
                break;
            case 1:
            default:
                if (isViewVisible(withText("Ch2"))) {
                    waitForView(withText("Ch2"), isEnabled()).perform(click());
                }
                Thread.sleep(1 * 1000);
                onView(ViewMatchers.withText("Ch1")).check(matches(isEnabled()));
                break;
        }
    }

    private static final int TIMEOUT_MILLIS = 30000;
    private static final int UI_CHECK_MILLIS = 100;

    public static ViewInteraction waitForView(Matcher<View> view, Matcher<View> condition) throws InterruptedException {
        for (int t = 0; t <= TIMEOUT_MILLIS; t += UI_CHECK_MILLIS) {
            try {
                Thread.sleep(UI_CHECK_MILLIS);
                return onView(view).check(matches(condition));
            } catch (Error | Exception e) {
                // no-op
            }
        }
        throw new AssertionFailedError(
                String.format("UI Element not found: %s, %s",
                        view.toString(),
                        condition.toString()));
    }

    public static ViewAction waitFor(long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for " + millis + " milliseconds";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

}
