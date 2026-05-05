package com.miis.horusendoview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.miis.horusendoview.UnitTest.isViewVisible;
import static com.miis.horusendoview.UnitTest.waitForView;
import static com.miis.horusendoview.action.RecyclerViewItemActions.clickChildViewWithId;
import static com.miis.horusendoview.action.RecyclerViewItemActions.getRecyclerViewItemCount;
import static com.miis.horusendoview.action.RecyclerViewItemActions.getTextFromItemWithId;

import static org.hamcrest.CoreMatchers.allOf;

import android.app.Application;
import android.graphics.Rect;
import android.view.View;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

public class UnitTest_LoginFragment {
    @Rule
    public ActivityScenarioRule<MainActivity> mainActivityRule
            = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setup() throws Exception {
        // navigate to log fragment
        waitForView(withId(R.id.user), isEnabled()).perform(click());
    }

    @Test
    // T4U11-1
    public void testLogin() throws Exception {
        List<LoginTestCase> testCases = Arrays.asList(
                new LoginTestCase("public", "public", true),
                new LoginTestCase("admin", "123456", true),
                new LoginTestCase("service", "123456", true),
                new LoginTestCase("adminNG", "123456", false),
                new LoginTestCase("admin", "12345", false),
                new LoginTestCase("public", "", false),
                new LoginTestCase("", "public", false),
                new LoginTestCase("", "", false)
        );

        // 登出 & 顯示登入畫面
        waitForView(withId(R.id.logout), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).perform(click());
        Thread.sleep(1000);
        mainActivityRule.getScenario().onActivity(activity -> activity.showLoginFragment(null));

        for (LoginTestCase testCase : testCases) {
            String strAccount = testCase.account;
            String strPW = testCase.password;
            Timber.d("[testLogin] %s, %s", strAccount, strPW);

            // 填入帳號密碼
            onView(allOf(withId(R.id.account), isAssignableFrom(AppCompatEditText.class), isDisplayed()))
                    .perform(clearText(), typeText(strAccount), closeSoftKeyboard());
            onView(withId(R.id.password))
                    .perform(clearText(), typeText(strPW), closeSoftKeyboard());

            // 點擊登入
            waitForView(withId(R.id.login), isEnabled()).perform(click());
            Thread.sleep(500);

            // 驗證登入結果
            final boolean expectedResult = testCase.result;
            mainActivityRule.getScenario().onActivity(activity -> {
                MyApplication myApplication = (MyApplication) activity.getApplication();
                UserTbData loginUserTbData = myApplication.getLoginUserTbData();

                Assert.assertEquals(expectedResult, loginUserTbData != null);
                if (loginUserTbData != null) {
                    Assert.assertEquals("Incorrect account", strAccount, loginUserTbData.getAccount());
                    Assert.assertEquals("Incorrect password", strPW, loginUserTbData.getPassword());
                    myApplication.setLoginUserTbData(null);
                }
            });
            Thread.sleep(1000);

            // 如果有dialog，把它關掉
            clickDialogOkIfPresent();

            // UI 刷新
            waitForView(withId(R.id.ibSettings), isEnabled()).perform(click());
            waitForView(withId(R.id.user), isEnabled()).perform(click());
        }
    }


    private void clickDialogOkIfPresent() {
        try {
            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            UiObject okButton = device.findObject(new UiSelector().text("OK"));
            if (okButton.exists() && okButton.isEnabled()) {
                okButton.click();
            }
        } catch (Exception e) {
            // 忽略任何例外，讓測試繼續
        }
    }

    public class LoginTestCase {
        public final String account;
        public final String password;
        public String new_password;
        public final boolean result;

        public LoginTestCase(String account, String password, boolean expectedSuccess) {
            this.account = account;
            this.password = password;
            this.result = expectedSuccess;
        }

        public LoginTestCase(String account, String password, String new_password, boolean expectedSuccess) {
            this.account = account;
            this.password = password;
            this.new_password = new_password;
            this.result = expectedSuccess;
        }
    }

//    @Test
//    // T4U11-2: not ready
//    public void testCreateAccount() throws Exception {
//
//        // 登出 & 顯示登入畫面
//        waitForView(withId(R.id.logout), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).perform(click());
//        Thread.sleep(1000);
//        mainActivityRule.getScenario().onActivity(activity -> activity.showLoginFragment(null));
//
//        // 填入帳號密碼
//        onView(allOf(withId(R.id.account), isAssignableFrom(AppCompatEditText.class), isDisplayed()))
//                .perform(clearText(), typeText("admin"), closeSoftKeyboard());
//        onView(withId(R.id.password))
//                .perform(clearText(), typeText("123456"), closeSoftKeyboard());
//
//        // 點擊登入
//        waitForView(withId(R.id.login), isEnabled()).perform(click());
//        Thread.sleep(500);
//
//        //顯示user list
//        waitForView(withId(R.id.ibSettings), isDisplayed()).perform(click());
//        Thread.sleep(1000);
//        waitForView(withId(R.id.user), isDisplayed()).perform(click());
//        Thread.sleep(1000);
//        mainActivityRule.getScenario().onActivity(activity -> {
//            View view = activity.getMemberFragment().getView();
//            Timber.d("[testCreateAccount] Olive: view = " + view);
//            if (view != null) {
//                Rect rect = new Rect();
//                boolean shown = view.getGlobalVisibleRect(rect);
//                Timber.d("[testCreateAccount] Olive:getGlobalVisibleRect: " + shown + ", rect: " + rect);
//            }
//        });
//
//
//        // 點擊Add User
//        waitForView(withId(R.id.addUser), isDisplayed()).perform(click());
//
//        // 填入帳號密碼
//        String account = "user";
//        String password = "ab123456";
//        onView(allOf(withId(R.id.account), isAssignableFrom(AppCompatEditText.class), isDisplayed()))
//                .perform(clearText(), typeText(account), closeSoftKeyboard());
//        onView(withId(R.id.password))
//                .perform(clearText(), typeText(password), closeSoftKeyboard());
//        onView(withId(R.id.repeatPassword))
//                .perform(clearText(), typeText(password), closeSoftKeyboard());
//
//        //選擇User
//        onView(withText("User"))
//                .inRoot(RootMatchers.isPlatformPopup())
//                .perform(click());
//
//        // 點擊Create
//        waitForView(withId(R.id.create), isDisplayed()).perform(click());
//
//        Thread.sleep(1000);
//
//        mainActivityRule.getScenario().onActivity(activity -> {
//            Application application = activity.getApplication();
//
//            // 如果你有自定義 Application 類別
//            MyApplication myApplication = (MyApplication) application;
//
//            UserTbData user = null;
//            try {
//                if (myApplication != null) {
//                    user = myApplication.getMyRoomDatabase().userTbDataDao().findByAccountAndPassword(account.toLowerCase(), password);
//                }
//            } catch (Exception e) {
//                Timber.e(e);
//            }
//
//            Assert.assertTrue("No Test Account", user != null);
//        });
//
//
//    }
//
//    @Test
//    // T4U11-3: not ready
//    public void testDeleteAccount() throws Exception {
//
//    }

    @Test
    // T4U11-4
    public void testChangePassword() throws Exception {
        List<LoginTestCase> testCases = Arrays.asList(
                new LoginTestCase("admin", "123456", "ab123456", true),
                new LoginTestCase("admin", "","", false)
        );
        // 登出 & 顯示登入畫面
        waitForView(withId(R.id.logout), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)).perform(click());
        Thread.sleep(1000);
        mainActivityRule.getScenario().onActivity(activity -> activity.showLoginFragment(null));

        for (LoginTestCase testCase : testCases){
            // 填入帳號密碼
            onView(allOf(withId(R.id.account), isAssignableFrom(AppCompatEditText.class), isDisplayed()))
                    .perform(clearText(), typeText("admin"), closeSoftKeyboard());
            onView(withId(R.id.password))
                    .perform(clearText(), typeText("123456"), closeSoftKeyboard());

            // 點擊登入
            waitForView(withId(R.id.login), isEnabled()).perform(click());
            Thread.sleep(500);

            Timber.d("[testChangePassword] %s, %s, %s", testCase.account, testCase.password, testCase.new_password);
            UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            // 等待動畫完成 (可能需要根據實際情況調整等待時間)
            uiDevice.waitForIdle();

//            onView(withId(R.id.oldPassword))
//                    .perform(clearText(), typeText(testCase.password), closeSoftKeyboard());
            onView(withId(R.id.newPassword))
                    .perform(clearText(), typeText(testCase.new_password), closeSoftKeyboard());
            onView(withId(R.id.confirmPassword))
                    .perform(clearText(), typeText(testCase.new_password), closeSoftKeyboard());

            // 點擊Save
            waitForView(withId(R.id.save), isEnabled()).perform(click());
            Thread.sleep(500);

            // 驗證修改結果
            mainActivityRule.getScenario().onActivity(activity -> {
                Application application = activity.getApplication();

                // 如果你有自定義 Application 類別
                MyApplication myApplication = (MyApplication) application;

                UserTbData user = null;
                try {
                    if (myApplication != null) {
                        user = myApplication.getMyRoomDatabase().userTbDataDao().findByAccountAndPassword(testCase.account.toLowerCase(), testCase.new_password);
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                if (user != null) {
                    Timber.d("[testChangePassword] new %s, %s", user.getAccount(), user.getPassword());

                    Assert.assertEquals("Incorrect password", testCase.new_password, user.getPassword());

                    //復原
                    user.setPassword(testCase.password);
                    myApplication.getMyRoomDatabase().userTbDataDao().update(user);
                }
                Assert.assertEquals(testCase.result, user != null);


            });
            Thread.sleep(1000);

            // 如果有dialog，把它關掉
            clickDialogOkIfPresent();


//            // UI 刷新
//            waitForView(withId(R.id.ibSettings), isEnabled()).perform(click());
//            waitForView(withId(R.id.user), isEnabled()).perform(click());
            mainActivityRule.getScenario().onActivity(activity -> activity.showLoginFragment(null));


        }
    }

}
