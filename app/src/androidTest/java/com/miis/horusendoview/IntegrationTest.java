package com.miis.horusendoview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.miis.horusendoview.UnitTest.isViewVisible;
import static com.miis.horusendoview.UnitTest.random;
import static com.miis.horusendoview.UnitTest.removeAllFiles;
import static com.miis.horusendoview.UnitTest.waitForView;

import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.miis.horusendoview.action.GetTextAction;
import com.miis.horusendoview.activity.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import timber.log.Timber;

public class IntegrationTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mainActivityRule
            = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setup() {
        //MyRoomDatabase.getDatabase(InstrumentationRegistry.getInstrumentation().getContext());
        //todo: make sure do we need to reset status by pressing home
        //mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        //mDevice.pressHome();
        //todo: make sure do we need to reset status by pressing home
    }

    @After
    public void tearDown() {
    }

    @Test
    //TI3a-1
    public void testNSnapshot() throws Exception {
        //輸入patient ID
        final LocalDateTime now = LocalDateTime.now();
        final String strTime = now.format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        mainActivityRule.getScenario().onActivity(activity -> {
            final MyApplication myApplication = activity.getMyApplication();
            myApplication.setPatientId(strTime);
        });
        Thread.sleep(1000);

        waitForView(withId(R.id.cameraView), isDisplayed());

        removeAllFiles();
        try { setChannel(1); } catch(Exception e) {/*no op*/}

        // take n snapshot
        int n = 3;//test n pictures
        int i;
        for(i = 1; i <= n; i++) {
            waitForView(withId(R.id.ibLiveView), isEnabled()).perform(click());
            Thread.sleep(1 * 1000);

            waitForView(withId(R.id.snapshot), isEnabled()).perform(click());
            //check green dot appear and disappear
            waitForView(withId(R.id.promptSnapshot), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE));
            waitForView(withId(R.id.promptSnapshot), withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE));

            Thread.sleep(2 * 1000);

            // navigate to data manager, check output
            waitForView(withId(R.id.ibDataManagement), isEnabled()).perform(click());
            Thread.sleep(1 * 1000);

            // double click on first item, this should be the folder holding the picture
            if ( i == 1) {
                onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                        .perform(RecyclerViewActions.actionOnItemAtPosition(0, doubleClick()));
                Thread.sleep(1 * 1000);
            }

            // double click the item,
            onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(i, doubleClick()));

            // get text from filename textview
            GetTextAction getTextAction = new GetTextAction();
            onView(withId(R.id.previewFileInfo)).perform(getTextAction);
            String fileName = getTextAction.getText();

            // split filename to [yyyy_MM_dd_HHmm] _NNNN. [png]
            String[] filename_fragment = fileName.split("_\\d{4}\\.");

            //check filename extension
            assertTrue("Filename extension is not png", filename_fragment[1].equals("png"));

            // double click on first item, this should be the picture
            boolean moreThanFiles = true;
            try {
                onView(allOf(withId(R.id.recyclerView), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                        .perform(RecyclerViewActions.actionOnItemAtPosition(i + 1, doubleClick()));
            } catch (Exception e) {
                moreThanFiles = false;
            }
            assertFalse("There's more than 3 files in the folder", moreThanFiles);
        }
    }

    @Test
    //TI5a-1 ; TI5a-2
    public void Live_ViewControl_Channel() throws Exception {
        waitForView(withId(R.id.cameraView), isDisplayed());

        for (int i = 0; i < 3; i++) {
            setChannel(2);
            Thread.sleep(5 * 1000);
            setChannel(1);
            Thread.sleep(5 * 1000);
        }

        //check there's no error #2000 message
        Thread.sleep(11 * 1000);
        onView(ViewMatchers.withId(R.id.tvMsg)).check(ViewAssertions.doesNotExist());
    }

    public static void setChannel(int channel) throws Exception {
        switch(channel){
            case 2:
                if(isViewVisible(withText("Ch1"))) {
                    waitForView(withText("Ch1"), isEnabled()).perform(click());
                }
                Thread.sleep(1 * 1000);
                onView(ViewMatchers.withText("Ch2")).check(matches(isEnabled()));
                break;
            case 1:
            default:
                if(isViewVisible(withText("Ch2"))) {
                    waitForView(withText("Ch2"), isEnabled()).perform(click());
                }
                Thread.sleep(1 * 1000);
                onView(ViewMatchers.withText("Ch1")).check(matches(isEnabled()));
                break;
        }
    }



}
