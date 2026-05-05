package com.miis.horusendoview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.miis.horusendoview.UnitTest.waitForView;

import android.view.View;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.miis.horusendoview.activity.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UnitTest_EngineeringFragment {

    @Rule
    public ActivityScenarioRule<MainActivity> mainActivityRule
            = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp(){
        mainActivityRule.getScenario().onActivity(activity -> {
            activity.getBinding().engineering.setVisibility(View.VISIBLE);
            activity.onClick(activity.getBinding().engineering);
        });
    }

    @Test
    //T4U4-2
    public void UnitTestUSB() throws Exception {
        waitForView(withId(R.id.value_usb), withText("Pass"));
    }

    @Test
    //T4U4-3
    public void UnitTestSN() throws Exception{
        waitForView(withId(R.id.value_serialnumber), isEnabled()).perform(longClick());
        Thread.sleep(2000);
        onView(ViewMatchers.withId(R.id.etSerialNumber)).perform(typeText("12345678"), closeSoftKeyboard());
        waitForView(withId(R.id.btnSerialNumber), isEnabled()).perform(click());

        Thread.sleep(1500);
        waitForView(withId(R.id.value_serialnumber), withText("12345678"));

    }

    @Test
    //T4U4-5
    public void UnitTestAudio() throws Exception{
        waitForView(withId(R.id.btnAudioPlay), isEnabled()).perform(click());
        Thread.sleep(8000);
    }
}
