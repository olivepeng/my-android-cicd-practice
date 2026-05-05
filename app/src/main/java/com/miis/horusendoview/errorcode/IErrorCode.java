package com.miis.horusendoview.errorcode;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;

import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.ActivityMainBinding;

import timber.log.Timber;

public interface IErrorCode {
    String TAG = IErrorCode.class.getSimpleName();

    String getCode();
    String getMessage(Context context);
    int getIcon();
    Location getLocation();

    enum Location{
        Center,
        Lower_Left
    }

    static void showErrorCode(Activity activity, IErrorCode errorCode){
        if(errorCode.getLocation() == Location.Center){
            showErrorCodeDialogSet(activity, errorCode);
        }else{ //Location.Lower_Left
            showErrorCodeView(activity, errorCode);
        }
    }

    @UiThread
    static void removeErrorCodeView(Activity activity, IErrorCode errorCode){
        if(!(activity instanceof MainActivity)){
            Timber.e("[showErrorCodeView] the context is not MainActivity.");
            return;
        }else{
            final ActivityMainBinding binding = ((MainActivity) activity).getBinding();

            final LinearLayout llErrorCode = binding.llErrorCode;
            final View viewWithTag = llErrorCode.findViewWithTag(errorCode);
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    llErrorCode.removeView(viewWithTag);
                }
            });
        }
    }

    @UiThread
    static View showErrorCodeView(Activity activity, IErrorCode errorCode){
        Log.i(TAG,"[showErrorCodeView] "+errorCode.getCode());
        if(!(activity instanceof MainActivity)){
            Timber.e("[showErrorCodeView] the context is not MainActivity.");
            return  null;
        }

        final ActivityMainBinding binding = ((MainActivity) activity).getBinding();

        final LinearLayout llErrorCode = binding.llErrorCode;
        if(llErrorCode.findViewWithTag(errorCode)!=null){
            Timber.w("[showErrorCodeView] Error code has been displayed. "+errorCode.getCode());
            return null;
        }
        final LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.dialog_error_code_lower_left, null).findViewById(R.id.layout_item);

        view.setTag(errorCode);

        ImageView ivIcon=view.findViewById(R.id.ivIcon);
        TextView tvMsg=view.findViewById(R.id.tvMsg);

        final Runnable runnableShowLowerLeftErrorCode = new Runnable() {
            @Override
            public void run() {
                llErrorCode.addView(view,0);

                final ViewGroup.LayoutParams p = view.getLayoutParams();
                if (p instanceof LinearLayout.LayoutParams) {
                    ((LinearLayout.LayoutParams)p).setMargins(4,4,4,4);
                }else {
                    Timber.e("[showErrorCodeView] layout_item isn't instanceof LinearLayout");
                }

                if(errorCode instanceof Error){
                    tvMsg.setText(errorCode.getCode());
                }else{
                    tvMsg.setText("");
                }
                ivIcon.setImageResource(errorCode.getIcon());
            }
        };

        binding.getRoot().post(runnableShowLowerLeftErrorCode);

        if(errorCode instanceof Error) {
            view.findViewById(R.id.ivClose).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((Error) errorCode).enable = false;
                    removeErrorCodeView(activity, errorCode);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ((Error) errorCode).enable = true;
                        }
                    }, 5 * 60 * 1000L);
                }
            });
        }

        return view;
    }

    //做AlertDialog的設定
    @AnyThread
    static void showErrorCodeDialogSet(Activity activity, IErrorCode errorCode) {
        final Runnable r = new Runnable(){

            @Override
            public void run() {
                Log.i(TAG,"[showErrorCodeDialogSet] "+errorCode.getCode());

                //新增一個AlertDialog
                final AlertDialog.Builder builder=new AlertDialog.Builder(activity);
                //選用特定的layout
                View view=activity.getLayoutInflater().inflate(R.layout.dialog_error_code,null);
                builder.setView(view);

                //建構dialog並顯示
                final AlertDialog dialog = builder.create();
                //点击dialog之外的区域禁止取消dialog
                dialog.setCanceledOnTouchOutside(false);

                Window window = dialog.getWindow();
                //这一句消除白块
                window.setBackgroundDrawable(new BitmapDrawable());


                //设置AlertDialog显示的位置
                //原文链接：https://blog.csdn.net/yh18668197127/article/details/84975023
                WindowManager.LayoutParams wlp =dialog.getWindow().getAttributes();
                wlp.gravity = Gravity.CENTER;

                //取得控制元件
                ImageView ivIcon=view.findViewById(R.id.ivIcon);
                ivIcon.setImageResource(errorCode.getIcon());

                TextView tvMsg=view.findViewById(R.id.tvMsg);
                tvMsg.setText(errorCode.getCode());

                Button btn = view.findViewById(R.id.btnPositive);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Timber.d( "[showErrorCodeDialogSet] onClick cancel");
                        //對話框消失
                        dialog.cancel();

                    }
                });

                dialog.show();

                //设置AlertDialog的宽高,注意这行代码必须放在dialog.show()的后面,否则无效
                dialog.getWindow().setLayout(995,415);

            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            activity.runOnUiThread(r);
        }

    }
}
