package com.miis.horusendoview.fragment.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.activity.MainActivity;

import timber.log.Timber;

public abstract class BaseFragment extends Fragment implements View.OnClickListener {

    @Nullable
    private MainActivity mainActivity;
    private boolean isFirstCreate = true;

    /**
     * 是否不要使用返回鍵回上一頁
     *
     * @return true = 不要使用返回鍵回上一頁
     */
    public abstract boolean isNotAddToBackStack();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null; // Your implementation here
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFirstCreate = true;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        isFirstCreate = false;
    }

    public boolean isFirstCreate() {
        return isFirstCreate;
    }

    @Nullable
    public MainActivity getMainActivity() {
        return mainActivity;
    }


    @Nullable
    public MyApplication getMyApplication() {
        try {
            Context context = getContext();
            if (context != null) {
                return (MyApplication) context.getApplicationContext();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }
}

