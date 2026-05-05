package com.miis.horusendoview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.miis.horusendoview.MyApplication.FOLDER_NAME;
import static com.miis.horusendoview.UnitTest.waitForView;
import static com.miis.horusendoview.action.RecyclerViewItemActions.clickChildViewWithId;
import static com.miis.horusendoview.action.RecyclerViewItemActions.getRecyclerViewItemCount;
import static com.miis.horusendoview.action.RecyclerViewItemActions.getTextFromItemWithId;
import static com.miis.horusendoview.fragment.CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.os.Environment;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.miis.horusendoview.action.GetTextAction;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.dicom.ImageToDicomService;

import org.apache.commons.io.FileUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UnitTest_DataManagementFragment {

    @Rule
    public ActivityScenarioRule<MainActivity> mainActivityRule
            = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    //T4U2-1
    public void UnitTestExportToUSB() throws Exception{
        //Insert USB stick

        waitForView(withId(R.id.user), isEnabled()).perform(click());
        waitForView(withId(R.id.logout), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).perform(click());
        Thread.sleep(1000);

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

        //切換到data management
        waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());
        Thread.sleep(1*1000);
        waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));

        //Copy a folder
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.checkBoxClick)));
        Thread.sleep(1*1000);
        waitForView(withId(R.id.basicExport), isEnabled()).perform(click());
        Thread.sleep(1*1000);
        waitForView(withId(R.id.password), isDisplayed()).perform(clearText());
        onView(ViewMatchers.withId(R.id.password)).perform(typeText("ab123456"), closeSoftKeyboard());
        waitForView(withText("Confirm"), isDisplayed()).perform(click());
        waitForView(withText("Close"), isDisplayed()).perform(click());

        //Copy a file
        // double click on first item, this should be the folder holding the picture
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, doubleClick()));
        Thread.sleep(1*1000);
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.checkBoxClick)));
        waitForView(withId(R.id.basicExport), isEnabled()).perform(click());
        Thread.sleep(1*1000);
        waitForView(withId(R.id.password), isDisplayed()).perform(clearText());
        onView(ViewMatchers.withId(R.id.password)).perform(typeText("ab123456"), closeSoftKeyboard());
        waitForView(withText("Confirm"), isDisplayed()).perform(click());
        waitForView(withText("Close"), isDisplayed()).perform(click());

        waitForView(withId(R.id.user), isEnabled()).perform(click());
        waitForView(withId(R.id.logout), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).perform(click());

    }

    @Test
    //T4U2-2
    public void UnitTestFrameCapture() throws Exception{
        //要接內視鏡
        final int Number_of_tests= 5;

//        removeAllFiles();

//        waitForView(withId(R.id.ibLiveView), isEnabled()).perform(click());

        //輸入patient ID
        final LocalDateTime now = LocalDateTime.now();
        final String strTime = now.format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId(strTime);
        });
        Thread.sleep(1000);

        //start record
        waitForView(withId(R.id.record), isEnabled()).perform(click());
        Thread.sleep(5*1000);
        //stop record
        waitForView(withId(R.id.record), isEnabled()).perform(click());

        // navigate to data manager, check output
        waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());
        Thread.sleep(1*1000);

        // double click on first item, this should be the folder holding the video
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, doubleClick()));
        Thread.sleep(1*1000);
        waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));

//        final GetRecyclerViewItemsCount getRecyclerViewItemsCount = new GetRecyclerViewItemsCount();

        for (int i = 0; i <Number_of_tests ; i++) {
//            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
//                    .perform(getRecyclerViewItemsCount);
            final int beginningSize = getRecyclerViewItemCount(onView(withId(R.id.recyclerView))); // 獲取 item 數量

            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(beginningSize-1, click()));
            Thread.sleep(1*1000);

            waitForView(withId(R.id.screenshot), isEnabled()).perform(click());
            Thread.sleep(2*1000);

//            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
//                    .perform(getRecyclerViewItemsCount);
            final int afterSize = getRecyclerViewItemCount(onView(withId(R.id.recyclerView))); // 獲取 item 數量
            assertEquals("the size of RecycleView is incorrect.", beginningSize+1, afterSize);
        }
    }

    @Test
    //T4U2-3
    public void UnitTestImagePreview() throws Exception{
        final int Number_of_tests= 5;

        //removeAllFiles();

        //輸入patient ID
        final LocalDateTime now = LocalDateTime.now();
        final String strTime = now.format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId(strTime);
        });
        Thread.sleep(1000);

        waitForView(withId(R.id.ibLiveView), isEnabled()).perform(click());
        waitForView(withId(R.id.snapshot), isEnabled()).perform(click());

        // navigate to data manager, check output
        waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());

        for (int i = 0; i < Number_of_tests; i++) {
            waitForView(withId(R.id.search), isDisplayed()).perform(click());
            Thread.sleep(500);
            waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));

            // double click on first item, this should be the folder holding the picture
            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, doubleClick()));
            Thread.sleep(1*1000);

            // double click on first item, this should be the video
            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

//            final GetRecyclerViewItemsCount getRecyclerViewItemsCount = new GetRecyclerViewItemsCount();
//            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
//                    .perform(getRecyclerViewItemsCount);
            AtomicReference<String> itemName = new AtomicReference<>();
            onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition(0, getTextFromItemWithId(R.id.name, itemName)));
            final String nameByIndex = itemName.get();

            // get text from filename textview
            GetTextAction getTextAction = new GetTextAction();
            onView(withId(R.id.previewFileInfo)).perform(getTextAction);
            String fileName=getTextAction.getText();

            //check filename extension
            assertEquals("Filename extension is not png", ".jpg", fileName.substring(fileName.lastIndexOf(".")));
            assertEquals("File name is incorrect.", nameByIndex, fileName);
            onView(withId(R.id.previewImg)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    //T4U2-4
    public void UnitTestVideoPlay() throws Exception{
        final int Number_of_tests= 5;
        //removeAllFiles();

        //輸入patient ID
        final LocalDateTime now = LocalDateTime.now();
        final String strTime = now.format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId(strTime);
        });
        Thread.sleep(1000);

        waitForView(withId(R.id.ibLiveView), isEnabled()).perform(click());
        //start record
        waitForView(withId(R.id.record), isEnabled()).perform(click());
        Thread.sleep(5*1000);
        //stop record
        waitForView(withId(R.id.record), isEnabled()).perform(click());
        Thread.sleep(2*1000);

        // navigate to data manager, check output
        waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());
        for (int i = 0; i < Number_of_tests; i++) {
            waitForView(withId(R.id.search), isDisplayed()).perform(click());
            waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));

            // double click on first item, this should be the folder holding the video
            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, doubleClick()));
            Thread.sleep(1 * 1000);

            // single click on first item, this should be the video
            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

//            final GetRecyclerViewItemsCount getRecyclerViewItemsCount = new GetRecyclerViewItemsCount();
//            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
//                    .perform(getRecyclerViewItemsCount);

            AtomicReference<String> itemName = new AtomicReference<>();
            onView(withId(R.id.recyclerView)
            ).perform(RecyclerViewActions.actionOnItemAtPosition(0, getTextFromItemWithId(R.id.name, itemName)));
            final String nameByIndex = itemName.get();


            // get text from filename textview
            GetTextAction getTextAction = new GetTextAction();
            onView(withId(R.id.previewFileInfo)).perform(getTextAction);
            String fileName = getTextAction.getText();

            //check filename extension
            assertEquals("Filename extension is not mp4", ".mp4", fileName.substring(fileName.lastIndexOf(".")));
            assertEquals("File name is incorrect.", nameByIndex, fileName);
            onView(withId(R.id.playerView)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }

    }

    @Test
    //T4U2-5
    public void UnitTestDelete() throws Exception{

        waitForView(withId(R.id.user), isEnabled()).perform(click());
        waitForView(withId(R.id.logout), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).perform(click());
        Thread.sleep(1000);

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

        //輸入patient ID
        final LocalDateTime now = LocalDateTime.now();
        final String strTime = now.format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId(strTime);
        });
        Thread.sleep(1000);

        //至少要兩個資料夾
        waitForView(withId(R.id.ibLiveView), isEnabled()).perform(click());
        waitForView(withId(R.id.snapshot), isEnabled()).perform(click());
        waitForView(withId(R.id.snapshot), isEnabled()).perform(click());
        Thread.sleep(1*1000);
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId(strTime+"1");
        });
        Thread.sleep(1000);
        waitForView(withId(R.id.snapshot), isEnabled()).perform(click());
        waitForView(withId(R.id.snapshot), isEnabled()).perform(click());
        Thread.sleep(1*1000);
        waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());
        waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));

        //a folder
        int beforeSize = getRecyclerViewItemCount(
                onView(
                        allOf(
                                withId(R.id.recyclerView),
                                withParent(ViewMatchers.withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")))
                        )
                )
        ); // 獲取 item 數量

        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.checkBoxClick)));

        waitForView(withText("Delete"), isEnabled()).perform(click());
        waitForView(withText("Yes"), isEnabled()).perform(click());
        Thread.sleep(1000);
        waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));
        int afterSize = getRecyclerViewItemCount(onView(
                allOf(
                        withId(R.id.recyclerView),
                        withParent(ViewMatchers.withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")))
                )
        )); // 獲取 item 數量
        assertEquals("the size of RecycleView is incorrect.", beforeSize-1, afterSize);

        // double click on first item
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, doubleClick()));
        Thread.sleep(1000);
        waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));
        beforeSize = getRecyclerViewItemCount(onView(
                allOf(
                        withId(R.id.recyclerView),
                        withParent(ViewMatchers.withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")))
                )
        )); // 獲取 item 數量

        //a file
        onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.checkBoxClick)));

        waitForView(withText("Delete"), isEnabled()).perform(click());
        waitForView(withText("Yes"), isEnabled()).perform(click());
        Thread.sleep(1000);
        waitForView(withId(R.id.loadingLayout), withEffectiveVisibility(ViewMatchers.Visibility.GONE));
        afterSize = getRecyclerViewItemCount(onView(
                allOf(
                        withId(R.id.recyclerView),
                        withParent(ViewMatchers.withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")))
                )
        )); // 獲取 item 數量
        assertEquals("the size of RecycleView is incorrect.", beforeSize-1, afterSize);

    }

    @Test
    //T4U2-6
    public void UnitTestJPG2DICOM() throws Exception {
        //輸入patient ID
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId("DICOM-test");
        });

        //拍一張照片
        waitForView(withId(R.id.snapshot), isEnabled()).perform(click());

        // 找出 filesDirFile 底下最新的檔案，為 srcPath
        final Instant nowInstant = Instant.now();
        final LocalDate nowDate = nowInstant.atZone(ZoneId.systemDefault()).toLocalDate();
        final String dateStr = nowDate.format(
                DateTimeFormatter.ofPattern(
                        DIR_NAME_DATE_FORMAT_PATTERN,
                        Locale.ENGLISH
                )
        );
        final File filesDirFile = new File(Environment.getExternalStoragePublicDirectory(FOLDER_NAME).getAbsolutePath()
                + File.separator + "public" + "_" + "DICOM-test" + "_" + dateStr);
        File[] imageFiles = filesDirFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg"));
        File latestImageFile = null;
        if (imageFiles != null && imageFiles.length > 0) {
            Arrays.sort(imageFiles, Comparator.comparingLong(File::lastModified).reversed());
            latestImageFile = imageFiles[0];
        }
        assertNotNull("找不到任何 jpg 檔案", latestImageFile);
        String srcPath = latestImageFile.getAbsolutePath();

        //desPath 為 dicomTempDir + srcPath 檔名（副檔名改為 dcm）
        final File dicomTempDir = new File(Environment.getExternalStoragePublicDirectory(FOLDER_NAME).getAbsolutePath() + File.separator + MyApplication.DICOM_TEMP_NAME);
        String dicomFileName = latestImageFile.getName().replaceAll("\\.jpg$", ".dcm");
        File desFile = new File(dicomTempDir, dicomFileName);
        String desPath = desFile.getAbsolutePath();
        Timber.d("[UnitTestJPG2DICOM]dateStr" + dateStr);
        if (desFile.exists()) {
            try {
                if (!desFile.delete()) {
                    Timber.d("delete file fail: " + desFile.getAbsolutePath());
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        try {
            if (!dicomTempDir.exists()) {
                dicomTempDir.mkdirs();
            }
        } catch (Exception e) {
            Timber.e(e);
        }


        //執行轉檔
        ImageToDicomService imageToDicomService = new ImageToDicomService();
        imageToDicomService.initMetaData("Test Patient", "Test Patient0821_1", "F", "50", "19500101", "20100101", "123", "Study's Description", "123", "20100101", "123", "999", "1", "008");
        final Map<Integer, String> dicomInfo = new HashMap<Integer, String>();
        dicomInfo.put(Tag.PatientID, "Test");
        dicomInfo.put(Tag.PatientAge, "1");
        dicomInfo.put(Tag.StudyDate, "20010203");
        dicomInfo.put(Tag.PatientSex, "F");
        dicomInfo.put(Tag.StudyDescription, "Description");
        imageToDicomService.setMetaDate(dicomInfo);
        imageToDicomService.convertJpg2Dcm(srcPath, desPath);

        // 驗證 DICOM Tag
        try (DicomInputStream dis = new DicomInputStream(desFile)) {
            Attributes dicomAttributes = dis.readDataset(-1, -1);

            //  取得常用 Tag 值
            String patientID = dicomAttributes.getString(Tag.PatientID);
            String studyDate = dicomAttributes.getString(Tag.StudyDate);
            String patientSex = dicomAttributes.getString(Tag.PatientSex);
            String patientAge = dicomAttributes.getString(Tag.PatientAge);
            String description = dicomAttributes.getString(Tag.StudyDescription);

            //  驗證內容正確
            assertEquals("Test", patientID);
            assertEquals("20010203", studyDate);
            assertEquals("F", patientSex);
            assertEquals("1", patientAge);
            assertEquals("Description", description);


        } catch (Exception e) {
            fail("解析 DICOM 時發生錯誤: " + e.getMessage());
        }

        FileUtils.deleteQuietly(desFile);
    }


}
