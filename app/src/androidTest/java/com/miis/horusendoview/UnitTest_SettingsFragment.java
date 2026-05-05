package com.miis.horusendoview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.miis.horusendoview.MyApplication.FOLDER_NAME;
import static com.miis.horusendoview.UnitTest.isViewVisible;
import static com.miis.horusendoview.UnitTest.waitForView;

import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.os.storage.StorageVolume;
import android.text.format.DateFormat;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.blankj.utilcode.util.FileUtils;
import com.miis.horusendoview.action.GetTextAction;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.manager.MyStorageManager;
import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.manager.SystemPropertiesUnit;
import com.miis.horusendoview.type.BootAutoDeleteFileType;
import com.miis.horusendoview.type.MaximumVideoDurationType;
import com.miis.horusendoview.type.StandbyNotificationTimeType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import timber.log.Timber;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(AndroidJUnit4.class)
public class UnitTest_SettingsFragment {
    //要接USB隨身碟

    @Rule
    public ActivityScenarioRule<MainActivity> mainActivityRule
            = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setup() throws Exception {
        // navigate to settings view
        waitForView(withId(R.id.ibSettings), isEnabled()).perform(click());
    }

    @Test
    //T4U3-1
    public void testGetNotificationTime() throws Exception {
        waitForView(withId(R.id.standbyNotificationTime10MinRadioButton), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.standbyNotificationTime30MinRadioButton), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.standbyNotificationTime60MinRadioButton), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.standbyNotificationTimeNeverRadioButton), isDisplayed()).check(matches(isEnabled()));
        StandbyNotificationTimeType standbyNotificationTimeType = SharedPreferencesManager.getInstance().getStandbyNotificationTimeType();
        switch (standbyNotificationTimeType) {
            case MIN_10:
                waitForView(withId(R.id.standbyNotificationTime10MinRadioButton), isChecked());
                break;
            case MIN_30:
                waitForView(withId(R.id.standbyNotificationTime30MinRadioButton), isChecked());
                break;
            case MIN_60:
            default:
                waitForView(withId(R.id.standbyNotificationTime60MinRadioButton), isChecked());
                break;
            case NEVER:
                waitForView(withId(R.id.standbyNotificationTimeNeverRadioButton), isChecked());
                break;
        }
    }

    @Test
    // T4U3-2
    public void testSetNotificationTime() throws Exception {
        int[] radioButtonIdList = {R.id.standbyNotificationTime10MinRadioButton,
                R.id.standbyNotificationTime30MinRadioButton,
                R.id.standbyNotificationTime60MinRadioButton,
                R.id.standbyNotificationTimeNeverRadioButton};
        for (int id : radioButtonIdList) {
            waitForView(withId(id), isEnabled()).perform(click());
            waitForView(withId(id), isChecked());
            StandbyNotificationTimeType standbyNotificationTimeType = SharedPreferencesManager.getInstance().getStandbyNotificationTimeType();
            switch (id) {
                case R.id.standbyNotificationTime10MinRadioButton:
                    assertEquals(standbyNotificationTimeType, StandbyNotificationTimeType.MIN_10);
                    break;
                case R.id.standbyNotificationTime30MinRadioButton:
                    assertEquals(standbyNotificationTimeType, StandbyNotificationTimeType.MIN_30);
                    break;
                case R.id.standbyNotificationTime60MinRadioButton:
                default:
                    assertEquals(standbyNotificationTimeType, StandbyNotificationTimeType.MIN_60);
                    break;
                case R.id.standbyNotificationTimeNeverRadioButton:
                    assertEquals(standbyNotificationTimeType, StandbyNotificationTimeType.NEVER);
                    break;
            }
        }
    }

    @Test
    //T4U3-3
    public void testGetMaximumVideoDuration() throws Exception {
        waitForView(withId(R.id.maximumVideoDuration30MinRadioButton), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.maximumVideoDuration60MinRadioButton), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.maximumVideoDuration90MinRadioButton), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.maximumVideoDuration120MinRadioButton), isDisplayed()).check(matches(isEnabled()));
        MaximumVideoDurationType maximumVideoDurationType = SharedPreferencesManager.getInstance().getMaximumVideoDurationType();
        if (maximumVideoDurationType == null) {
            maximumVideoDurationType = MaximumVideoDurationType.MIN_30;
        }
        switch (maximumVideoDurationType) {
            case MIN_30:
                waitForView(withId(R.id.maximumVideoDuration30MinRadioButton), isChecked());
                break;
            case MIN_60:
                waitForView(withId(R.id.maximumVideoDuration60MinRadioButton), isChecked());
                break;
            case MIN_90:
            default:
                waitForView(withId(R.id.maximumVideoDuration90MinRadioButton), isChecked());
                break;
            case MIN_120:
                waitForView(withId(R.id.maximumVideoDuration120MinRadioButton), isChecked());
                break;
        }
    }

    @Test
    // T4U3-4
    public void testSetMaximumVideoDuration() throws Exception {
        int[] radioButtonIdList = {R.id.maximumVideoDuration30MinRadioButton,
                R.id.maximumVideoDuration60MinRadioButton,
                R.id.maximumVideoDuration90MinRadioButton,
                R.id.maximumVideoDuration120MinRadioButton};
        for (int id : radioButtonIdList) {
            waitForView(withId(id), isEnabled()).perform(click());
            waitForView(withId(id), isChecked());
            MaximumVideoDurationType maximumVideoDurationType = SharedPreferencesManager.getInstance().getMaximumVideoDurationType();
            if (maximumVideoDurationType == null) {
                maximumVideoDurationType = MaximumVideoDurationType.MIN_30;
            }
            switch (id) {
                case R.id.maximumVideoDuration30MinRadioButton:
                    assertEquals(maximumVideoDurationType, MaximumVideoDurationType.MIN_30);
                    break;
                case R.id.maximumVideoDuration60MinRadioButton:
                    assertEquals(maximumVideoDurationType, MaximumVideoDurationType.MIN_60);
                    break;
                case R.id.maximumVideoDuration90MinRadioButton:
                default:
                    assertEquals(maximumVideoDurationType, MaximumVideoDurationType.MIN_90);
                    break;
                case R.id.maximumVideoDuration120MinRadioButton:
                    assertEquals(maximumVideoDurationType, MaximumVideoDurationType.MIN_120);
                    break;
            }
        }
    }

    @Test
    //T4U3-5
    public void testSetSystemTime() throws Exception {
        //先設定成12小時制
        waitForView(withId(R.id.hours12), isEnabled()).perform(click());

        //紀錄現在時間
        final long currentTimeMillis = System.currentTimeMillis();

        LocalDateTime dateTime = LocalDateTime.of(2024, 7, 4, 7, 4, 10);
        long millis = dateTime.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        SystemClock.setCurrentTimeMillis(millis);

        // test set date
        waitForView(withId(R.id.textClock), isEnabled()).perform(click());
        waitForView(withId(R.id.dateLayout), isEnabled()).perform(click());
        //todo: find a way to access on the textview shows "Month YEAR" to setup a year.
        //      Tried string matching using RE, partial string matching and access through ID, none
        //      of the above works.
        //      --Morgan Huang 20241008
        waitForView(withText("10"), isEnabled()).perform(click());
        Thread.sleep(1000);
        waitForView(withText("Confirm"), isEnabled()).perform(click());

        // test set time
        waitForView(withId(R.id.timeLayout), isEnabled()).perform(click());
        waitForView(withText("12"), isEnabled()).perform(click());
        waitForView(withText("00"), isEnabled()).perform(click());
        waitForView(withText("PM"), isEnabled()).perform(click());
        Thread.sleep(1000);
        waitForView(withText("Confirm"), isEnabled()).perform(click());
        Thread.sleep(1000);
        waitForView(withText("Confirm"), isEnabled()).perform(click());

        // check system time is set
        LocalDateTime now = LocalDateTime.now();
        assertEquals(now.format(DateTimeFormatter.ofPattern("dd")), "10");
        assertEquals(now.format(DateTimeFormatter.ofPattern("HH")), "12");
        assertEquals(now.format(DateTimeFormatter.ofPattern("mm")), "00");

        //設定回原本時間
        SystemClock.setCurrentTimeMillis(currentTimeMillis);
    }

    @Test
    //T4U3-6
    public void testShowSerialNumber() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String serialnumber = SystemPropertiesUnit.getSystemProperty(SystemPropertiesUnit.PROPERTY_KEY_SERIALNUMBER);
        final String serialNumberLebelString = appContext.getString(R.string.serial_number_str, serialnumber);
        waitForView(withId(R.id.serialNumber), isDisplayed()).check(matches(withText(serialNumberLebelString)));
    }


    @Test
    //T4U3-7
    public void testGetFreeDiskSpace() throws Exception {
        //設置文件路徑和名稱
        File file = Environment.getExternalStoragePublicDirectory("UnitTest");
        try {
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        file = new File(file,"UnitTestGetFreeDiskSpace_100MB_File.txt");
        if (file.exists()) {
            file.delete();
            //Thread.sleep(2*1000);
            //Switch pages
            waitForView(withId(R.id.ibLiveView), isEnabled()).perform(click());
            waitForView(withId(R.id.ibSettings), isEnabled()).perform(click());
        }


        final GetTextAction getTextAction = new GetTextAction();
        onView(withId(R.id.freeDisk)).perform(getTextAction);
        String text = getTextAction.getText();
        final int indexGB = text.indexOf(" GB");
        final String sFreeSize = text.substring("Free disk：".length(), indexGB);
        final float fFreeSize = Float.parseFloat(sFreeSize);

        double freeDisk = 0.0;
        try {
            freeDisk = FileUtils.getFsAvailableSize(Environment.getExternalStorageDirectory().getAbsolutePath()) / 1024.0 / 1024.0 / 1024.0;
        } catch (Exception e) {
            Timber.e(e);
        }
        freeDisk = Math.ceil(freeDisk * 10.0) / 10.0; //一位小數並無條件進位（CEILING）
        assertEquals("the value is incorrect.", String.format("%.1f", freeDisk), sFreeSize);


        //1. 產生一個100MB file
        //設置文件大小
        long fileSize = 102 * 1024 * 1024; //100MB

        //生成文件
        FileOutputStream fos = new FileOutputStream(file);

        //寫入數據
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
        String data = "This is an 100MG file.";
        for (int i = 0; i < fileSize; i += data.length()) {
            writer.write(data);
        }

        //關閉文件
        writer.close();

        //Thread.sleep(2*1000);
        //Switch pages
        waitForView(withId(R.id.ibLiveView), isEnabled()).perform(click());
        waitForView(withId(R.id.ibSettings), isEnabled()).perform(click());

        //2.	view values
        onView(ViewMatchers.withId(R.id.freeDisk))
                .check(ViewAssertions.matches(ViewMatchers.withText(text.replace(sFreeSize, String.format("%.1f", fFreeSize - 0.1)))));

        file.delete();
    }

    /*
    @Test
    //T4U3-8
    public void testShowSoftwareVersion() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final String build_time = (new SimpleDateFormat("yyMMdd")).format(new Date(BuildConfig.BUILD_TIME));
        final String swVersion = appContext.getString(R.string.software_version_str, build_time, BuildConfig.VERSION_NAME);
        waitForView(withId(R.id.softwareVersion), isDisplayed()).check(matches(withText(swVersion)));
    }
    */

    @Test
    //T4U3-9
    public void testExportLogFile() throws Exception {
        //要插隨身碟

        waitForView(withId(R.id.user), isEnabled()).perform(click());
        waitForView(withId(R.id.logout), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).perform(click());
        Thread.sleep(1000);

//        mainActivityRule.getScenario().onActivity(activity -> {
//            activity.showLoginFragment(null);
//        });

        //登入帳號
        onView(allOf(
                withId(R.id.account),
                isAssignableFrom(AppCompatEditText.class),
                isDisplayed()
        )).perform(clearText());
        onView(allOf(
                withId(R.id.account),
                isAssignableFrom(AppCompatEditText.class),
                isDisplayed()
        )).perform(typeText("service"), closeSoftKeyboard());
        onView(ViewMatchers.withId(R.id.password)).perform(clearText());
        onView(ViewMatchers.withId(R.id.password)).perform(typeText("123456"), closeSoftKeyboard());
        waitForView(withId(R.id.login), isEnabled()).perform(click());
        Thread.sleep(1000);

        waitForView(withId(R.id.ibSettings), isEnabled()).perform(click());

        final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
        if (storageList == null || storageList.size() <= 0) {
            fail("no USB stick");
        }

        if (isViewVisible(withText(R.id.btnExportLog)) == false) {
            onView(withId(R.id.exportLogArrow)).perform(click());
        }
        Thread.sleep(1 * 1000);
        waitForView(withId(R.id.btnExportLog), isDisplayed()).perform(click());

        Thread.sleep(1 * 1000);
        waitForView(withId(R.id.password), isDisplayed()).perform(clearText());
        onView(ViewMatchers.withId(R.id.password)).perform(typeText("ab123456"), closeSoftKeyboard());
        waitForView(withText("Confirm"), isDisplayed()).perform(click());
        Thread.sleep(2 * 1000);
        waitForView(withText("Close"), isDisplayed()).perform(click());

        final StorageVolume storageVolume = storageList.get(0);
        LocalDate today = LocalDate.now();// 取得今天日期
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");// 定義格式器
        String dateStr = today.format(formatter);// 格式化日期
        String fileName = dateStr + "-00-00_" + dateStr + "-23-59_logs.zip";// 組合字串
        final File file = new File(storageVolume.getDirectory(), FOLDER_NAME + File.separator + fileName);
        assertTrue("There is no log file in the USB stick.", file.exists());
    }

    @Test
    //T4U3-11
    public void testGetDateTimeDisplayFormat() throws Exception {
        waitForView(withId(R.id.hours12), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.hours24), isDisplayed()).check(matches(isEnabled()));

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        boolean hourFormat = DateFormat.is24HourFormat(context);
        if(hourFormat){
            waitForView(withId(R.id.hours24), isChecked());
        }else{
            waitForView(withId(R.id.hours12), isChecked());
        }
    }

    @Test
    // T4U3-12
    public void testSetDateTimeDisplayFormat() throws Exception {
        int[] radioButtonIdList = {R.id.hours12,
                R.id.hours24};
        for (int id : radioButtonIdList) {
            waitForView(withId(id), isEnabled()).perform(click());
            waitForView(withId(id), isChecked());
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            boolean hourFormat = DateFormat.is24HourFormat(context);
            switch (id) {
                case R.id.hours12:
                    assertFalse(hourFormat);
                    break;
                case R.id.hours24:
                default:
                    assertTrue(hourFormat);
                    break;
            }
        }
    }

    @Test
    //T4U3-13
    public void testGetAutomaticallyDeleteFiles() throws Exception {
        waitForView(withId(R.id.autoDeleteFile1Day), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.autoDeleteFile7Days), isDisplayed()).check(matches(isEnabled()));
        waitForView(withId(R.id.autoDeleteFile30Days), isDisplayed()).check(matches(isEnabled()));
        BootAutoDeleteFileType bootAutoDeleteFileType = SharedPreferencesManager.getInstance().getBootAutoDeleteFileType();
        switch (bootAutoDeleteFileType) {
            case DAY_1:
                waitForView(withId(R.id.autoDeleteFile1Day), isChecked());
                break;
            case DAY_7:
                waitForView(withId(R.id.autoDeleteFile7Days), isChecked());
                break;
            case DAY_30:
            default:
                waitForView(withId(R.id.autoDeleteFile30Days), isChecked());
                break;
        }
    }

    @Test
    // T4U3-14
    public void testSetAutomaticallyDeleteFiles() throws Exception {
        int[] radioButtonIdList = {R.id.autoDeleteFile1Day,
                R.id.autoDeleteFile7Days,
                R.id.autoDeleteFile30Days};
        for (int id : radioButtonIdList) {
            waitForView(withId(id), isEnabled()).perform(click());
            waitForView(withId(id), isChecked());
            BootAutoDeleteFileType bootAutoDeleteFileType = SharedPreferencesManager.getInstance().getBootAutoDeleteFileType();
            switch (id) {
                case R.id.autoDeleteFile1Day:
                    assertEquals(bootAutoDeleteFileType, BootAutoDeleteFileType.DAY_1);
                    break;
                case R.id.autoDeleteFile7Days:
                    assertEquals(bootAutoDeleteFileType, BootAutoDeleteFileType.DAY_7);
                    break;
                case R.id.autoDeleteFile30Days:
                default:
                    assertEquals(bootAutoDeleteFileType, BootAutoDeleteFileType.DAY_30);
                    break;
            }
        }
    }

}
