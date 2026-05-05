package com.miis.horusendoview.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.os.storage.StorageVolume;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.FragmentEngineeringBinding;
import com.miis.horusendoview.manager.MyStorageManager;
import com.miis.horusendoview.manager.SystemPropertiesUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EngineeringFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EngineeringFragment extends Fragment {

    @Nullable
    private FragmentEngineeringBinding binding;

    @NonNull
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    @NonNull
    private int color_value_default=0x49454F;

    public EngineeringFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment EngineeringFragment.
     */
    public static EngineeringFragment newInstance() {
        EngineeringFragment fragment = new EngineeringFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentEngineeringBinding.inflate(inflater, container, false);
        }
        new Thread(){
            @Override
            public void run() {
                initView();
            }
        }.start();
        return binding.getRoot();
    }

    private void initView() {
        binding.btnExitEngineering.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Timber.d("btnExitEngineering: onClick");
                final FragmentActivity activity = getActivity();
                if(activity!=null && activity instanceof MainActivity){
                    final MainActivity mainActivity = (MainActivity) activity;
                    mainActivity.getBinding().engineering.setVisibility(View.GONE);
                    mainActivity.onClick(mainActivity.getBinding().ibLiveView);
                }
            }
        });

        color_value_default=binding.valueEthernet.getCurrentTextColor();

        setEthernetLayout();
        setSerialNumberLayout();
        setAudioLayout();
        setUSBExportLayout();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MyStorageManager.getInstance().addListener(myStorageManagerListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUSBExportisEnable();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden){

        }else{
            FragmentActivity activity = getActivity();
            if (activity instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) activity;
                mainActivity.setTabBtnSelected(mainActivity.getBinding().engineering);
                if (binding != null) {
                    Timber.d("[onHiddenChanged]");
                    binding.getRoot().postDelayed(() -> mainActivity.setTabBtnClickable(true), MainActivity.TAB_BTN_CLICKABLE_ON_DELAY);
                }
            }
        }
    }

    private void setEthernetLayout(){
        updateEthernetStatus();
        final FragmentActivity activity = getActivity();
        if(activity==null){
            Timber.w("[setEthernetLayout] activity==null");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(connectivityManager!=null){
                connectivityManager.requestNetwork(new NetworkRequest.Builder().build(),
                        new ConnectivityManager.NetworkCallback(){
                            @Override
                            public void onAvailable(Network network) {
                                super.onAvailable(network);
                                Timber.d("[NetworkCallback] onAvailable");
                                updateEthernetStatus();
                            }

                            @Override
                            public void onLost(Network network) {
                                super.onLost(network);
                                Timber.d("[NetworkCallback] onLost");
                                updateEthernetStatus();
                            }
                        });
            }
        }
    }

    private void updateEthernetStatus(){
        if(binding==null){
            Timber.w("[updateEthernetStatus] binding==null");
            return;
        }
        final String tmp = getLocalIpAddress();
        final TextView tvEthernet = binding.valueEthernet;

        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if(tvEthernet!=null){
                    if(tmp==null ){
                        tvEthernet.setText(getResources().getString(R.string.unavailable));
                        tvEthernet.setTextColor(color_value_default);
                    }else{
                        tvEthernet.setText("IP: "+tmp);
                        tvEthernet.setTextColor(Color.RED);
                    }
                }
            }
        });
    }

    /**
     * 獲取本地 ip
     *@return
     */
    @Nullable
    public String getLocalIpAddress(){
        try{
            String ipv4=null;
            // 取得機器所有網路介面
            List<NetworkInterface> nilist= Collections.list(NetworkInterface.getNetworkInterfaces());
            for(NetworkInterface ni:nilist){
                // 取得網卡所有的 IP 地址
                List<InetAddress>ialist= Collections.list(ni.getInetAddresses());
                for(InetAddress address:ialist){
                    // 只取 IPv4 地址
                    ipv4=address.getHostAddress();
                    if(!address.isLoopbackAddress() && address instanceof Inet4Address){
                        return ipv4;
                    }
                }
            }
        }catch(SocketException ex){
            Timber.e( "[getLocalIpAddress]SocketException="+ex.toString());
        }catch (Exception e){
            Timber.e( "[getLocalIpAddress]"+e.toString());
        }
        return null;
    }

    private void setSerialNumberLayout(){
        if(binding==null){
            Timber.w("[setSerialNumberLayout] binding==null");
            return;
        }
        final String serialnumber = SystemPropertiesUnit.getSystemProperty(SystemPropertiesUnit.PROPERTY_KEY_SERIALNUMBER);
        final TextView tvSerialnumber = binding.valueSerialnumber;
        final EditText etSerialNumber = binding.etSerialNumber;
        final Button btnSerialNumber = binding.btnSerialNumber;
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if(tvSerialnumber!=null && serialnumber!=null){
                    tvSerialnumber.setText(serialnumber);
                }
            }
        });
        tvSerialnumber.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(btnSerialNumber.getVisibility()==View.VISIBLE){
                    etSerialNumber.setVisibility(View.GONE);
                    btnSerialNumber.setVisibility(View.GONE);
                }else {
                    final String serialnumber = SystemPropertiesUnit.getSystemProperty(SystemPropertiesUnit.PROPERTY_KEY_SERIALNUMBER);
                    if (serialnumber.isEmpty()) {
                        etSerialNumber.setVisibility(View.VISIBLE);
                        btnSerialNumber.setVisibility(View.VISIBLE);
                    }
                    else if (Check_Doorkey(3) == 3) {
                        etSerialNumber.setVisibility(View.VISIBLE);
                        btnSerialNumber.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            }
        });
        etSerialNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Timber.d( "[afterTextChanged]"+s);
            }
        });
        etSerialNumber.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    Timber.d("onKey: 按下回车键");
                    setSerialNumber();
                    return true;
                }
                return false;
            }
        });
        btnSerialNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSerialNumber();
            }
        });
    }

    public static int Check_Doorkey(int type){
        String MAGIC_KEY1 = "!$3a068c8324a30eefcc214529dc24f81498c54762a87022bc2#acae126cb52eeacb05fbfa90867d08557a4e18572b1fbe305fa2fe8b3908271eb2504e8$";
        String MAGIC_KEY2 = "!@3b307918d44368b3f36067c64ebd3ceebde049a266925d008#5965b606bc0bb784ab712743f68b47e31c0c0dba2fc9d02948968ab0dd3d5ae42a9a89f@";
        String FILENAME = null;

        final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
        if(storageList==null){
            return 0;
        }

        for (StorageVolume storage:storageList) {
            final File usbDir = storage.getDirectory();
            StringBuilder text = new StringBuilder();

            if (type == 1) {
                FILENAME = "MAGIC_KEY1.txt";
            }
            else if (type == 2) {
                FILENAME = "MAGIC_KEY2.txt";
            }
            else if (type == 3) {
                FILENAME = "MAGIC_KEY2.txt";
            }

            if(FILENAME != null) {
                File file = new File(usbDir, FILENAME);
                if(!file.exists()){
                    continue;
                }
                try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;

                    while ((line = br.readLine()) != null) {
                        text.append(line);
                    }
                    //br.close();
                } catch (IOException e) {
                    //TODO
                }

                if (type == 1) {
                    if (text.toString().equals(MAGIC_KEY1)) {
                        Timber.d( "key1");
                        return 1;
                    }
                }
                else if (type == 3) {
                    if (text.toString().equals(MAGIC_KEY2)) {
                        Timber.d( "key3");
                        return 3;
                    }
                }
            }
        }
        return 0;
    }

    private void setSerialNumber(){
        final String text = binding.etSerialNumber.getText().toString();
        SystemPropertiesUnit.setSystemProperty(SystemPropertiesUnit.PROPERTY_KEY_SERIALNUMBER, text);
        final String serialnumber = SystemPropertiesUnit.getSystemProperty(SystemPropertiesUnit.PROPERTY_KEY_SERIALNUMBER);
        binding.valueSerialnumber.setText(serialnumber);
        binding.etSerialNumber.setVisibility(View.GONE);
        binding.btnSerialNumber.setVisibility(View.GONE);
    }

    private void setAudioLayout() {
        final Context context = getContext();
        if(context==null){
            Timber.e("[setAudioLayout] context==null");
        }

        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.audio3s);
        mediaPlayer.setVolume(1,1);

        binding.btnAudioPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.start();
            }
        });

        //touch panel測試apk
        binding.textView22.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(Check_Doorkey(3)==3){
                    AlertDialog.Builder alertDialog =
                            new AlertDialog.Builder(context);
                    alertDialog.setTitle("Do you want to perform touch panel testing?");
                    alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PackageManager packageManager= context.getPackageManager();
                            Intent intent=packageManager.getLaunchIntentForPackage("com.the511plus.MultiTouchTester");
                            try {
                                startActivity(intent);
                            }catch (Exception e){
                                Timber.e("[setAudioLayout]"+e.getMessage());
                            }

                        }
                    });
                    alertDialog.setNegativeButton(R.string.no,(dialog, which) -> {
                        dialog.dismiss();
                    });
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                }
                return false;
            }
        });
    }

    private MyStorageManager.Listener myStorageManagerListener = new MyStorageManager.Listener() {
        @Override
        public void onStorageMounted(@NonNull File storageFile) {
            updateUSBExportisEnable();
        }

        @Override
        public void onStorageUnmounted(@NonNull File storageFile) {
            updateUSBExportisEnable();
        }
    };

    private void setUSBExportLayout() {
        binding.valueUsb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(Check_Doorkey(1) == 1){
//                    final Context context = getContext();
//                    if(context==null){
//                        Timber.e("[setUSBExportLayout] context==null");
//                    }

                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    /**知道要跳转应用的包命与目标Activity*/
                    ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.Settings");
                    intent.setComponent(componentName);
                    intent.putExtra("", "");//这里Intent传值
                    startActivity(intent);
                }
                return false;
            }
        });
    }

    private void updateUSBExportisEnable(){
        final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
        boolean isEnable=false;

        if(storageList!=null){
            for (StorageVolume storage:storageList) {
                final boolean b = verityPermission(storage.getDirectory());
                if(b){
                    isEnable=b;
                    break;
                }
            }
        }

        final TextView tvUSB = binding.valueUsb;
        boolean finalIsEnable = isEnable;
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if(tvUSB!=null){
                    if(finalIsEnable){
                        tvUSB.setText( "Pass");
                        tvUSB.setTextColor(color_value_default);
                    }else{
                        tvUSB.setText(getResources().getString(R.string.fail));
                        tvUSB.setTextColor(Color.RED);
                    }
                }
            }
        });

    }

    public static boolean verityPermission(File path){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss"); //測試檔案
        File testfile=new File(path, sf.format(new Date()));
        try{
            if (!testfile.createNewFile()){
                return false;
            }else{
                if(!testfile.delete()){
                    Timber.w("delete file fail: "+testfile.getAbsolutePath());
                }
                return true;
            }
        }catch (Exception e) {
            // TODO: handle exception
            Timber.w("Exception: "+ e.getMessage());
            return false;
        }
    }
}