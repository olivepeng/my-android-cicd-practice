package com.miis.horusendoview.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.herohan.uvcapp.ICameraHelper;
import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.FragmentCameraBinding;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import org.mockito.Mockito;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers; // Robolectric 提供的反射工具

import java.util.concurrent.TimeUnit;

import com.miis.horusendoview.errorcode.Error;

@RunWith(AndroidJUnit4.class)
// 在 @Config 裡面增加 application 參數
@Config(sdk = {30}, application = com.miis.horusendoview.MyApplication.class)
public class CameraFragmentTest {

    private ICameraHelper mockCameraHelper;

    @Before
    public void setUp() {
        // 1. 準備 Mock 物件
        mockCameraHelper = mock(ICameraHelper.class);
    }

    /**
     * 基本測試：確保 Fragment 成功啟動且不崩潰
     */
    @Test
    public void testFragmentNotNull() {
        FragmentScenario<CameraFragment> scenario = launchCameraFragment();

        scenario.onFragment(fragment -> {
            assertNotNull("Fragment should not be null", fragment);
        });
    }

    /**
     * 邏輯測試：當呼叫顯示提示且相機未開啟時，提示圖示應變為 VISIBLE
     */
    @Test
    public void testSetIvTipsShow_WhenCameraClosed_ShouldShowTips() {
        // 模擬相機是「關閉」狀態
        when(mockCameraHelper.isCameraOpened()).thenReturn(false);

        FragmentScenario<CameraFragment> scenario = launchCameraFragment();

        scenario.onFragment(fragment -> {
            // 2. 準備與注入 Mock (確保在 RESUMED 之後注入，避免被 initCameraHelper 覆蓋)
            ICameraHelper mockCameraHelper = mock(ICameraHelper.class);
            // 模擬相機關閉狀態
            when(mockCameraHelper.isCameraOpened()).thenReturn(false);

            ReflectionHelpers.setField(fragment, "iCameraHelper", mockCameraHelper);
//            ReflectionHelpers.setField(fragment, "mainHandler", new Handler(Looper.getMainLooper()));

            // 3. 執行測試目標方法
            fragment.setIvTipsShow(true);

            // 4. 【關鍵】強制讓 Looper 跑完剛才 post 出去的 Runnable
            org.robolectric.shadows.ShadowLooper.idleMainLooper();

            // 5. 驗證結果
            // 這裡我們直接用 findViewById 抓 View，確保沒抓錯物件
            View ivTips = fragment.getView().findViewById(R.id.ivTips);
            assertEquals("Tips view should be VISIBLE", View.VISIBLE, ivTips.getVisibility());
        });
    }

    /**
     * 邏輯測試：即使呼叫顯示提示，但若相機已開啟，提示圖示應變為 GONE
     */
    @Test
    public void testSetIvTipsShow_WhenCameraOpened_ShouldHideTips() {
        // 模擬相機是「開啟」狀態
        when(mockCameraHelper.isCameraOpened()).thenReturn(true);

        FragmentScenario<CameraFragment> scenario = launchCameraFragment();

        scenario.onFragment(fragment -> {
            fragment.setIvTipsShow(true);

            FragmentCameraBinding binding = ReflectionHelpers.getField(fragment, "binding");
            assertEquals("Tips view should be GONE", View.GONE, binding.ivTips.getVisibility());
        });
    }

    @Test
    public void testError2000Flow() {
        // 1. 啟動 Fragment 並注入必要的 Mock (使用先前討論的封裝方法)
        FragmentScenario<CameraFragment> scenario = launchCameraFragment();

        scenario.onFragment(fragment -> {
            ReflectionHelpers.setField(fragment, "iCameraHelper", mockCameraHelper);

            // --- 階段一：觸發 setIvTipsShow(false) ---
            fragment.setIvTipsShow(false);

            // 強制 Looper 執行立即任務（不包含延遲任務）
            ShadowLooper.idleMainLooper();

            // 驗證 enable 立即被設為 true
            // 注意：如果 Error 有名稱衝突，請使用完整路徑如 com.miis.horusendoview.model.Error
            assertTrue("初始狀態 enable 應為 true",
                    Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED.enable);

            // --- 階段二：模擬過了 10 秒 (OPEN_CAMERA_TIMEOUT_MILLIS) ---
            // 假設 OPEN_CAMERA_TIMEOUT_MILLIS 是 10000 毫秒
            ShadowLooper.idleMainLooper(11, TimeUnit.SECONDS);

            //此時 showE2000Runnable 應該已經跑完
            assertFalse("10秒後 enable 應變為 false",
                    Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED.enable);

        });
    }

    @After
    public void tearDown() {
        // 重置全域變數，避免影響下一個測試
        Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED.enable = true;
    }

    /**
     * 輔助方法：模擬關閉對話框
     * 因為單元測試通常不真的跳出對話框，我們直接呼叫對應的 Logic 方法
     */
    private void simulateCloseErrorDialog(CameraFragment fragment) {
        // 方式 A：如果該方法是 public
        // fragment.onCloseErrorDialog();

        // 方式 B：如果是 private，我們用反射呼叫它
        ReflectionHelpers.callInstanceMethod(fragment, "onCloseErrorDialog");
    }

    /**
     * 輔助方法：封裝啟動 Fragment 的複雜流程
     */
    private FragmentScenario<CameraFragment> launchCameraFragment() {
        // 先啟動到 INITIALIZED，讓我們有機會在 onViewCreated 前注入 Mock
        FragmentScenario<CameraFragment> scenario = FragmentScenario.launchInContainer(
                CameraFragment.class,
                null,
                R.style.Theme_Sa2_android,
                Lifecycle.State.INITIALIZED
        );

        scenario.onFragment(fragment -> {
            // 注入測試需要的「假零件」
            ReflectionHelpers.setField(fragment, "iCameraHelper", mockCameraHelper);
            ReflectionHelpers.setField(fragment, "mainHandler", new Handler(Looper.getMainLooper()));

            // 預先準備好空的 Runnable 避免 null
//            ReflectionHelpers.setField(fragment, "showE2000Runnable", (Runnable) () -> {});
        });

        // 推向 RESUMED 觸發生命週期
        scenario.moveToState(Lifecycle.State.RESUMED);

        // 強制執行 Handler 隊列中的任務
        ShadowLooper.idleMainLooper();

        return scenario;
    }


    @Test
    public void testFragmentWithoutNPE() {
        // 1. 啟動 Fragment，但停在 INITIALIZED 狀態
        // 這時 onViewCreated 尚未執行，我們有機會在它崩潰前「補洞」
        FragmentScenario<CameraFragment> scenario = FragmentScenario.launchInContainer(
                CameraFragment.class, null, R.style.Theme_Sa2_android, Lifecycle.State.INITIALIZED);

        scenario.onFragment(fragment -> {
            // 2. 準備 Mock 物件
            ICameraHelper mockHelper = Mockito.mock(ICameraHelper.class);
            Mockito.when(mockHelper.isCameraOpened()).thenReturn(true);

            // 3. 在 onViewCreated 執行前，強行把 Mock 塞進去
            // 這樣待會 onViewCreated 跑起來時，它抓到的就不會是 null
            org.robolectric.util.ReflectionHelpers.setField(fragment, "iCameraHelper", mockHelper);

            // 同樣補上 Handler 和其他可能需要的變數
            org.robolectric.util.ReflectionHelpers.setField(fragment, "mainHandler", new Handler(Looper.getMainLooper()));
            org.robolectric.util.ReflectionHelpers.setField(fragment, "showE2000Runnable", (Runnable) () -> {});
        });

        // 4. 現在零件都齊了，手動把生命週期推向 RESUMED
        // 這時會觸發 onViewCreated -> setIvTipsShow，但因為我們已經注入了 Mock，所以不會崩潰
        scenario.moveToState(Lifecycle.State.RESUMED);

        // 5. 確保排隊的非同步任務（Runnable）執行完畢
        org.robolectric.shadows.ShadowLooper.idleMainLooper();

        scenario.onFragment(fragment -> {
            assertNotNull(fragment);
        });
    }
}