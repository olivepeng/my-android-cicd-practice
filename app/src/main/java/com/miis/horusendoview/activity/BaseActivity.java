package com.miis.horusendoview.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.akexorcist.localizationactivity.core.LocalizationActivityDelegate;
import com.akexorcist.localizationactivity.core.OnLocaleChangedListener;
import com.blankj.utilcode.util.LanguageUtils;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import timber.log.Timber;

public class BaseActivity extends AppCompatActivity  {
    private AlertDialog loadDialog;

    private final LocalizationActivityDelegate localizationDelegate = new LocalizationActivityDelegate(this);

    @Override
    protected void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        localizationDelegate.addOnLocaleChangedListener(onLocaleChangedListener);
        localizationDelegate.onCreate();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        localizationDelegate.onResume(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        applyOverrideConfiguration(localizationDelegate.updateConfigurationLocale(newBase));
        super.attachBaseContext(newBase);
    }

    @Override
    public Context getApplicationContext() {
        return localizationDelegate.getApplicationContext(super.getApplicationContext());
    }

    @Override
    public Resources getResources() {
        return localizationDelegate.getResources(super.getResources());
    }


    public void setLanguage(@NonNull String language) {
        localizationDelegate.setLanguage(this, language);
    }

    public void setLanguage(@NonNull Locale locale) {
        localizationDelegate.setLanguage(this, locale);
    }

    @NonNull
    public Locale getCurrentLanguage() {
        return localizationDelegate.getLanguage(this);
    }


    /**
     * 顯示LOADING畫面
     */
    public synchronized void showLoadDialog() {
        showLoadDialog(null);
    }

    /**
     * 顯示LOADING畫面
     */
    public synchronized void showLoadDialog(@Nullable final String msg) {
        cancelLoadDialog();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (BaseActivity.this) {
                    Timber.d("showLoadDialog");
                    AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                    builder.setCancelable(false);

                    LinearLayout linearLayout = new LinearLayout(BaseActivity.this);
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.setGravity(Gravity.CENTER);

                    ProgressBar progressBar = new ProgressBar(BaseActivity.this);
                    LinearLayout.LayoutParams progressBarParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    progressBar.setLayoutParams(progressBarParams);
                    linearLayout.addView(progressBar);

                    if (msg != null && !msg.isEmpty()) {
                        TextView textView = new TextView(BaseActivity.this);
                        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        textView.setLayoutParams(textViewParams);
                        textView.setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.white));
                        textView.setTextSize(16);
                        textView.setText(msg);
                        linearLayout.addView(textView);
                    }

                    builder.setView(linearLayout);
                    loadDialog = builder.create();
                    if (loadDialog != null && loadDialog.getWindow() != null) {
                        loadDialog.getWindow().setBackgroundDrawableResource(R.drawable.load);
                        // loadDialog.getWindow().setDimAmount(0f);
                        loadDialog.show();
                    }
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            runOnUiThread(runnable);
        }
    }

    /**
     * 關閉LOADING畫面
     */
    public synchronized void cancelLoadDialog() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (BaseActivity.this) {
                    Timber.d("cancelLoadDialog");
                    if (loadDialog != null) {
                        loadDialog.dismiss();
                    }
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            runOnUiThread(runnable);
        }
    }

    @NonNull
    public MyApplication getMyApplication() {
        return (MyApplication) getApplication();
    }

    private final OnLocaleChangedListener onLocaleChangedListener = new OnLocaleChangedListener() {
        @Override
        public void onBeforeLocaleChanged() {

        }

        @Override
        public void onAfterLocaleChanged() {

        }
    };
}
