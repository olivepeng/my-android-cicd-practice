package com.miis.horusendoview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.miis.horusendoview.UnitTest.checkSeekBarValue;
import static com.miis.horusendoview.UnitTest.isViewVisible;
import static com.miis.horusendoview.UnitTest.random;
import static com.miis.horusendoview.UnitTest.setChannel;
import static com.miis.horusendoview.UnitTest.waitForView;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.*;

import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.miis.horusendoview.action.GetTextAction;
import com.miis.horusendoview.action.SetSeekBarAction;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.manager.SystemPropertiesUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import timber.log.Timber;

@RunWith(AndroidJUnit4.class)
public class UnitTest_CameraFragment {
    //Note: 要兩隻內視鏡

    @Rule
    public ActivityScenarioRule<MainActivity> mainActivityRule
            = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    // T4U1-1
    public void UnitTestSetPatientID() throws Exception {
        String[] input = new String[]{"ab1234", "1.a_",""};
        String[] expectedResults= new String[]{"ab1234", "1a", null};

        for (int i = 0; i < input.length; i++) {
            final String testId = input[i];
            Timber.d("[UnitTestSetPatientID] "+testId);

            // 1. 點擊 patientIdLayout 開啟輸入對話框
            onView(allOf(withId(R.id.patientIdLayout), isDisplayed()))
                    .perform(click());

            // 2. 等待並輸入文字到 EditText（dialog 中的 patientId）
            onView(withId(R.id.patientId))
                    .perform(clearText(), typeText(testId), closeSoftKeyboard());

            // 3. 按下 Save 按鈕（以文字匹配）
            onView(withText("Save"))
                    .perform(click());

            // 4. 等待 UI thread 執行完 setPatientIdToView()
//            mainActivityRule.getScenario().onActivity(activity -> {
//                CameraFragment fragment = activity.getCameraFragment();
//                if (fragment != null) {
//                    fragment.setPatientIdToView();
//                }
//            });


            if(expectedResults[i] == null){
                //驗證出現"Please Enter Patient ID"的dialog
                onView(withText("Please Enter Patient ID"))
                        .inRoot(isDialog())
                        .check(matches(isDisplayed()));
            }else {
                // 5. 驗證主畫面上的 TextView 是否正確顯示新的 ID
                onView(allOf(
                        withId(R.id.patientId),
                        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                        .check(matches(withText(expectedResults[i])));
            }
        }

    }

    @Test
    //T4U1-2
    //T4U1-3
    public void UnitTestRecordCycle() throws Exception {
        final int Number_of_tests= 50;

        //輸入patient ID
        final LocalDateTime now = LocalDateTime.now();
        final String strTime = now.format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId(strTime);
        });
        Thread.sleep(1000);

        waitForView(withId(R.id.cameraView), isDisplayed());
        for (int i = 0; i < Number_of_tests; i++) {
            Timber.d("[UnitTestRecordCycle] "+ i);

            if(isViewVisible(withText("REC"))) {
                waitForView(withId(R.id.record), isEnabled()).perform(click());
                waitForView(withText("REC"), withEffectiveVisibility(ViewMatchers.Visibility.GONE));
            }

            //start record
            waitForView(withId(R.id.record), isEnabled()).perform(click());

            //check UI update
            waitForView(withId(R.id.recordTime), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
            waitForView(withText("REC"), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));

            //check record time update
            waitForView(withId(R.id.recordTime), withText("00:00:00"));

            Thread.sleep(5*1000);

            //stop recording
            waitForView(withId(R.id.record), isEnabled()).perform(click());

            //check UI update
            waitForView(withText("REC"), withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE));
            waitForView(withId(R.id.recordingTimeLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));

            Thread.sleep(1*1000);


        }

        //check there's no error #2000 message
        Thread.sleep(11 * 1000);
        onView(ViewMatchers.withId(R.id.tvMsg)).check(ViewAssertions.doesNotExist());

        // navigate to data manager, check output
        waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());
        waitForView(withId(R.id.search), isDisplayed()).perform(click());
        Thread.sleep(1*1000);

        // double click on first item, this should be the folder holding the picture
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, doubleClick()));

        waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));

        // click on 100th item,
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(Number_of_tests-1, click()));

    }

    @Test
    //T4U1-4
    public void UnitTestTakePictureCycle() throws Exception {
        final int Number_of_tests= 100;

        //輸入patient ID
        final LocalDateTime now = LocalDateTime.now();
        final String strTime = now.format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId(strTime);
        });
        Thread.sleep(1000);

        waitForView(withId(R.id.cameraView), isDisplayed());

        for(int i=0; i<Number_of_tests; i++) {
            Timber.d("[UnitTestTakePictureCycle] "+i);

            onView(withId(R.id.snapshot)).perform(click());
            //check green dot appear and disappear
//            waitForView(withId(R.id.promptSnapshot), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
//            waitForView(withId(R.id.promptSnapshot), withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE));
//            Thread.sleep(2*1000);
        }

        // navigate to data manager, check output
        waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());
        waitForView(withId(R.id.search), isDisplayed()).perform(click());
        Thread.sleep(1*1000);

        // double click on first item, this should be the folder holding the picture
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, doubleClick()));
        Thread.sleep(1*1000);

        // click on 100th item,
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // get text from filename textview
        GetTextAction getTextAction = new GetTextAction();
        onView(withId(R.id.previewFileInfo)).perform(getTextAction);
        String fileName=getTextAction.getText();

        // filename to yyMMddHHmm_yyyy-MM-dd_HH-mm-ss-SSS.jpg

        //check filename extension
        final int indexOfExtension = fileName.lastIndexOf(".");
        final String extension = fileName.substring(indexOfExtension);
        assertTrue("Filename extension is not jpg", extension.equals(".jpg"));

        //check filename datetime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
        LocalDateTime currentDateTime = LocalDateTime.now();
        final int indexOfUnderline = fileName.indexOf("_");
        final String filename_datetime = fileName.substring(indexOfUnderline +1, indexOfExtension);
        LocalDateTime fileDateTime = LocalDateTime.parse(filename_datetime, formatter);
        Duration duration = Duration.between(fileDateTime, currentDateTime);
        long minutes = duration.toMinutes();
        assertTrue("Duration between filename date time and current is not less than 2 minutes", minutes < 2);

        //check the patient ID
        assertEquals("The folder name is incorrect",strTime, fileName.substring(0, indexOfUnderline) );


        // 預期不會超過100個
        boolean moreThan100Files=true;
        try {
            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(Number_of_tests, doubleClick()));
        }catch(Exception e){
            moreThan100Files=false;
        }
        assertFalse("There's more than 100 files in the folder", moreThan100Files);
    }

    @Test
    //T4U1-6
    public void UnitTestSetBrightness() throws Exception {
        waitForView(withId(R.id.cameraView), isDisplayed());
        if(isViewVisible(withId(R.id.cameraSettings))==false) {
            waitForView(withId(R.id.btnAdjust), isEnabled()).perform(click());
            waitForView(withId(R.id.cameraSettings), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
        }

        for (int i = 1; i <= 9; i++) {
            Timber.d("[UnitTestSetBrightness] "+i);
            waitForView(withId(R.id.brightnessSeekBar), isDisplayed()).perform(new SetSeekBarAction(i));
            Thread.sleep(1000);
            checkSeekBarValue(withId(R.id.brightnessSeekBar), i);
        }
    }

    @Test
    //T4U1-7
    public void UnitTestSetContrast() throws Exception {
        waitForView(withId(R.id.cameraView), isDisplayed());
        if(isViewVisible(withId(R.id.cameraSettings))==false) {
            waitForView(withId(R.id.btnAdjust), isEnabled()).perform(click());
            waitForView(withId(R.id.cameraSettings), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
        }

        for (int i = 1; i <= 9; i++) {
            waitForView(withId(R.id.contrastSeekBar), isDisplayed()).perform(new SetSeekBarAction(i));
            Thread.sleep(1000);
            checkSeekBarValue(withId(R.id.contrastSeekBar), i);
        }
    }

    @Test
    //T4U1-8
    public void UnitTestSetColorTemperature() throws Exception {
        waitForView(withId(R.id.cameraView), isDisplayed());
        if(isViewVisible(withId(R.id.cameraSettings))==false) {
            waitForView(withId(R.id.btnAdjust), isEnabled()).perform(click());
            waitForView(withId(R.id.cameraSettings), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
        }

        for (int i = 1; i <= 9; i++) {
            waitForView(withId(R.id.colourSeekBar), isDisplayed()).perform(new SetSeekBarAction(i));
            Thread.sleep(1000);
            checkSeekBarValue(withId(R.id.colourSeekBar), i);
        }
    }

    @Test
    //T4U1-9
    public void UnitTestSetSharpness() throws Exception {
        waitForView(withId(R.id.cameraView), isDisplayed());
        if(isViewVisible(withId(R.id.cameraSettings))==false) {
            waitForView(withId(R.id.btnAdjust), isEnabled()).perform(click());
            waitForView(withId(R.id.cameraSettings), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
        }

        for (int i = 1; i <= 9; i++) {
            waitForView(withId(R.id.sharpnessSeekBar), isDisplayed()).perform(new SetSeekBarAction(i));
            Thread.sleep(1000);
            checkSeekBarValue(withId(R.id.sharpnessSeekBar), i);
        }
    }

    @Test
    //T4U1-10
    public void UnitTestResetCameraSettings() throws Exception {
        waitForView(withId(R.id.cameraView), isDisplayed());
        if(isViewVisible(withId(R.id.cameraSettings))==false) {
            waitForView(withId(R.id.btnAdjust), isEnabled()).perform(click());
            waitForView(withId(R.id.cameraSettings), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
        }

        for (int i = 0; i < 5; i++) {
            int r=random(9,1);
            waitForView(withId(R.id.brightnessSeekBar), isEnabled()).perform(new SetSeekBarAction(r));
            r=random(9,1);
            waitForView(withId(R.id.contrastSeekBar), isEnabled()).perform(new SetSeekBarAction(r));
            r=random(9,1);
            waitForView(withId(R.id.colourSeekBar), isEnabled()).perform(new SetSeekBarAction(r));
            r=random(9,1);
            waitForView(withId(R.id.sharpnessSeekBar), isEnabled()).perform(new SetSeekBarAction(r));
            Thread.sleep(250);

            // test reset button
            waitForView(withId(R.id.reset), isEnabled()).perform(click());
            Thread.sleep(250);

            checkSeekBarValue(withId(R.id.brightnessSeekBar), 5);
            checkSeekBarValue(withId(R.id.contrastSeekBar), 5);
            checkSeekBarValue(withId(R.id.colourSeekBar), 5);
            checkSeekBarValue(withId(R.id.sharpnessSeekBar), 5);
        }

    }

    @Test
    //T4U1-11
    public void UnitTestChannelSwitchingCycle() throws Exception {
        final int Number_of_tests= 50;

        waitForView(withId(R.id.cameraView), isDisplayed());
        try{setChannel(1);}catch(Exception e){/*no op*/}
        Thread.sleep(12*1000);
        onView(ViewMatchers.withId(R.id.tvMsg)).check(ViewAssertions.doesNotExist());

        for (int i = 0; i < Number_of_tests; i++) {
            Timber.d("[UnitTestChannelSwitchingCycle] "+i);
            //Switch CH1-> CH2
            waitForView(withId(R.id.changeCamera), isEnabled()).perform(click());
            Thread.sleep(10*1000);
            onView(ViewMatchers.withText("Ch2")).check(matches(isEnabled()));
            onView(ViewMatchers.withId(R.id.tvMsg)).check(ViewAssertions.doesNotExist());

            //Switch CH2-> CH1
            waitForView(withId(R.id.changeCamera), isEnabled()).perform(click());
            Thread.sleep(10*1000);
            onView(ViewMatchers.withText("Ch1")).check(matches(isEnabled()));
            onView(ViewMatchers.withId(R.id.tvMsg)).check(ViewAssertions.doesNotExist());
        }
    }

    @Test
    //T4U1-12
    //T4U1-13
    public void UnitTestSetGetHCableUsageTimesCycle() throws Exception {
        final int Number_of_tests= 3;

        String prop_key, count;
        for (int i = 0; i < Number_of_tests; i++) {
            prop_key= "persist.horusendoview.ch1";
            SystemPropertiesUnit.setSystemProperty(prop_key, "100");
            count = SystemPropertiesUnit.getSystemProperty(prop_key);
            assertEquals("Get the value of H-Cable times is incorrect.", "100", count);

            prop_key="persist.horusendoview.ch2";
            SystemPropertiesUnit.setSystemProperty(prop_key, "100");
            count = SystemPropertiesUnit.getSystemProperty(prop_key);
            assertEquals("Get the value of H-Cable times is incorrect.", "100", count);

            Thread.sleep(5*1000);
        }

    }
}