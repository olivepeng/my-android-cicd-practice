package com.miis.horusendoview;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.akexorcist.localizationactivity.core.LocalizationApplicationDelegate;
import com.herohan.uvcapp.ICameraHelper;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.fragment.CameraFragment;
import com.miis.horusendoview.fragment.DataManagementFragment;
import com.miis.horusendoview.fragment.member.MemberFragment;
import com.miis.horusendoview.fragment.member.MemberListFragment;
import com.miis.horusendoview.fragment.member.MemberUserEditFragment;
import com.miis.horusendoview.manager.MyStorageManager;
import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.roomDataBase.MyRoomDatabase;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.type.UserRoleType;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();
    public final boolean FLAG_DEMO_ACCOUNT=true;
    public final String DEMO_ACCOUNT="public";

    /**
     * 圖片檔案副檔名
     */
    public static String IMG_FILE_EXTENSION = ".jpg";

    /**
     * 影片檔案副檔名
     */
    public static String VIDEO_FILE_EXTENSION = ".mp4";

    /**
     * json檔案副檔名
     */
    public static String JSON_FILE_EXTENSION = ".json";

    /**
     * 規格書要的存檔資料夾名稱
     */
    public static String FOLDER_NAME = "endoscope";

    public static String ZIP_TEMP_NAME = "zipTemp";

    public static String DICOM_TEMP_NAME = "dicomTemp";

    @Nullable
    private HandlerThread workHandlerThread = null;

    @Nullable
    private Handler workHandler = null;

    @Nullable
    private MainActivity mainActivity = null;

    /**
     * 存檔主要資料夾路徑
     */
    @NonNull
    private String mainDirPath = "";


    /**
     * 已登入會員資料
     */
    @Nullable
    private UserTbData loginUserTbData = null;
    private final Object loginUserTbDataLock = new Object();

    /**
     *病患id
     */
    @Nullable
    private String patientId = null;
    private final Object patientIdLock = new Object();

    private final LocalizationApplicationDelegate localizationDelegate = new LocalizationApplicationDelegate();

    @Override
    protected void attachBaseContext(Context base) {
        localizationDelegate.setDefaultLanguage(base, Locale.ENGLISH);
        super.attachBaseContext(localizationDelegate.attachBaseContext(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
		
		// 1. 偵測是否為 Robolectric 環境
        boolean isRobolectric = "robolectric".equals(android.os.Build.FINGERPRINT);


        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Timber.d("onCreate");
		
		// 2. 只有非測試環境才載入 SQLCipher 原生庫
        if (!isRobolectric) {
            SQLiteDatabase.loadLibs(this);
        }


        workHandlerThread = new HandlerThread(MyApplication.class.getSimpleName() + "_workHandlerThread");
        workHandlerThread.start();
        setWorkHandler(new Handler(workHandlerThread.getLooper()));


        //setMainDirPath(Environment.getExternalStoragePublicDirectory(FOLDER_NAME).getAbsolutePath());
		// 3. 處理路徑問題：測試環境通常無法存取 ExternalStoragePublicDirectory
        if (isRobolectric) {
            setMainDirPath(getFilesDir().getAbsolutePath());
        } else {
            setMainDirPath(Environment.getExternalStoragePublicDirectory(FOLDER_NAME).getAbsolutePath());
        }


        SharedPreferencesManager.getInstance().setMyApplication(this);


        MyStorageManager.getInstance().init(this);


        registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        MyRoomDatabase myRoomDatabase = getMyRoomDatabase();

        UserTbData adminData = null;
        try {
            adminData = myRoomDatabase.userTbDataDao().findByAccount("admin");
        } catch (Exception e) {
            Timber.e(e);
        }


        if (adminData == null) {
            UserTbData a = new UserTbData(
                    UUID.randomUUID().toString(),
                    "admin",
                    "123456",
                    ""
            );
            a.setRoleType(UserRoleType.ADMIN_USER);
            try {
                myRoomDatabase.userTbDataDao().insert(a);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        UserTbData serviceData =  null;
        try {
            serviceData = myRoomDatabase.userTbDataDao().findByAccount("service");
        } catch (Exception e) {
            Timber.e(e);
        }

        if (serviceData == null) {
            UserTbData a = new UserTbData(
                    UUID.randomUUID().toString(),
                    "service",
                    "123456",
                    ""
            );
            a.setRoleType(UserRoleType.SERVICE_USER);
            try {
                myRoomDatabase.userTbDataDao().insert(a);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        if(FLAG_DEMO_ACCOUNT){
            UserTbData demoData = null;
            try {
                demoData = myRoomDatabase.userTbDataDao().findByAccount(DEMO_ACCOUNT);
            } catch (Exception e) {
                Timber.e(e);
            }
            if (demoData == null) {
                UserTbData a = new UserTbData(
                        UUID.randomUUID().toString(),
                        DEMO_ACCOUNT,
                        DEMO_ACCOUNT,
                        ""
                );
                a.setRoleType(UserRoleType.ADVANCED_USER);
                try {
                    myRoomDatabase.userTbDataDao().insert(a);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }
    }



    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public Context getApplicationContext() {
        return localizationDelegate.getApplicationContext(super.getApplicationContext());
    }

    @Override
    public Resources getResources() {
        return localizationDelegate.getResources(getBaseContext(), super.getResources());
    }




    private final ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            Timber.d("activityLifecycleCallbacks -> onActivityCreated activity=" + activity);
            if (activity instanceof MainActivity) {
                setMainActivity((MainActivity) activity);
            }
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            Timber.d("activityLifecycleCallbacks -> onActivityStarted activity" + activity);

            //預設登入Demo帳號, 顯示live view.
            if(FLAG_DEMO_ACCOUNT) {
                try {
                    UserTbData user = getMyRoomDatabase().userTbDataDao().findByAccount(DEMO_ACCOUNT);//JerryLin

                    if (user == null) {
                        Timber.e("[onActivityCreated] Demo user not found.");
                        return;
                    }

                    setLoginUserTbData(user);

                    MainActivity mainActivity = (MainActivity) activity;
                    mainActivity.getBinding().ibLiveView.performClick();
                    mainActivity.setTabBtnSelected(mainActivity.getBinding().ibLiveView);

                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            Timber.d("activityLifecycleCallbacks -> onActivityResumed activity=" + activity);
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            Timber.d("activityLifecycleCallbacks -> onActivityPaused activity=" + activity);
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            Timber.d("activityLifecycleCallbacks -> onActivityStopped activity=" + activity);
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            Timber.d("activityLifecycleCallbacks -> onActivitySaveInstanceState activity=" + activity);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            Timber.d("activityLifecycleCallbacks -> onActivityDestroyed activity=" + activity);
            if (activity instanceof MainActivity) {
                setMainActivity(null);
            }
        }
    };

    @Nullable
    public MainActivity getMainActivity() {
        return mainActivity;
    }

    private void setMainActivity(@Nullable MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }


    @NonNull
    public String getMainDirPath() {
        return mainDirPath;
    }

    public void setMainDirPath(@NonNull String mainDirPath) {
        this.mainDirPath = mainDirPath;
    }

    @NonNull
    public MyRoomDatabase getMyRoomDatabase() {
        return MyRoomDatabase.getDatabase(this);
    }


    @Nullable
    public UserTbData getLoginUserTbData() {
        synchronized(loginUserTbDataLock) {
            return loginUserTbData;
        }
    }

    public void setLoginUserTbData(@Nullable UserTbData loginUserTbData) {
        synchronized(loginUserTbDataLock) {
            @Nullable UserTbData old = this.loginUserTbData;
            this.loginUserTbData = loginUserTbData;

            @Nullable final MainActivity mainActivity = getMainActivity();
            @Nullable CameraFragment cameraFragment = null;
            if (mainActivity != null) {
                mainActivity.checkUserIcon();
                cameraFragment = mainActivity.getCameraFragment();
                if (cameraFragment != null) {
                    cameraFragment.setLoginUserRoleTypeToView(old);
                }
            }

            @Nullable UserRoleType roleType = null;
            if (loginUserTbData != null) {
                roleType = loginUserTbData.getRoleType();
            }

            @Nullable ICameraHelper iCameraHelper = null;
            if (cameraFragment != null) {
                iCameraHelper = cameraFragment.getICameraHelper();
            }

            @Nullable DataManagementFragment dataManagementFragment = null;
            if (mainActivity != null) {
                dataManagementFragment = mainActivity.getDataManagementFragment();
            }

            if (roleType == null || roleType == UserRoleType.GUEST) {

                if (iCameraHelper != null && iCameraHelper.isRecording()) {
                    if (cameraFragment != null) {
                        cameraFragment.toggleVideoRecord(false);
                    }
                }

                setPatientId(null);

                if (dataManagementFragment != null) {
                    dataManagementFragment.setChildDirDisplayName(null);
                    dataManagementFragment.setPreviewFile(null);
                    dataManagementFragment.setProcedureFolderTbDataToView();
                    dataManagementFragment.cancelPlayerFullScreen();
                }

                if (mainActivity != null) {
                    mainActivity.showLoginFragment(null);
                }
            } else  if (roleType == UserRoleType.ADVANCED_USER) {
                @Nullable MemberFragment memberFragment = null;
                if (mainActivity != null) {
                    memberFragment = mainActivity.getMemberFragment();
                }

                if (memberFragment != null) {
                    memberFragment.showMemberUserEditFragment();
                }
            } else if (roleType ==  UserRoleType.ADMIN_USER ||
                    roleType ==  UserRoleType.SERVICE_USER) {
                @Nullable MemberFragment memberFragment = null;
                if (mainActivity != null) {
                    memberFragment = mainActivity.getMemberFragment();
                }
                if (memberFragment != null) {
                    memberFragment.showMemberListFragment();
                }
            }

//            if(FLAG_DEMO_ACCOUNT){
//                if(roleType ==  UserRoleType.ADMIN_USER && loginUserTbData.getAccount().equals(DEMO_ACCOUNT)){
//                    final int serialNumber = getMaxSerialNumber();
//                    Timber.d("[setLoginUserTbData]serialNumber="+serialNumber);
////                    setPatientId(String.format("%03d", serialNumber +1)); //JerryLin, disable for Sarnova
//                }
//            }

            if (old == null && loginUserTbData != null && mainActivity != null) {

                @Nullable final Handler workHandler = getWorkHandler();
                if (workHandler != null) {
                    workHandler.post(new Runnable(){
                        @Override
                        public void run() {
                            mainActivity.checkDiskInsufficiencyWithDeleteOldFolder(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            workHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mainActivity.checkDiskInsufficiencyWithDeleteOldFolder(null , null);
                                                }
                                            });
                                        }
                                    },
                                    null
                            );
                        }
                    });
                }


                if (cameraFragment != null) {
                    cameraFragment.setImageRotateTypeToRotation();
                }

                if (dataManagementFragment != null) {
                    dataManagementFragment.clearSearchCriteria();
                    dataManagementFragment.updateFileList();
                }
            }

        }
    }


    @Nullable
    public String getPatientId() {
        synchronized(patientIdLock) {
            return patientId;
        }
    }

    public void setPatientId(@Nullable String patientId) {
        synchronized(patientIdLock) {
            Log.i(TAG, "Set Patient ID: " +patientId );
            @Nullable final String old = this.patientId;
            this.patientId = patientId;

            @Nullable final MainActivity mainActivity = getMainActivity();

            @Nullable CameraFragment cameraFragment = null;
            if (mainActivity != null) {
                cameraFragment = mainActivity.getCameraFragment();
            }

            if (cameraFragment != null) {
                cameraFragment.setPatientIdToView();
                cameraFragment.checkPatientIdToChangePreviewImg(this.patientId , old , false);
            }
        }
    }

    @Nullable
    public Handler getWorkHandler() {
        return workHandler;
    }

    public void setWorkHandler(@Nullable Handler workHandler) {
        this.workHandler = workHandler;
    }

    //for demo account
    private int getMaxSerialNumber(){
        File dirFile=new File(getMainDirPath());
        File[] fileListTArr = new File[]{};

        try {
            fileListTArr = dirFile.listFiles(childFile -> {
                try {
                    if (!childFile.exists()) {
                        return false;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    if (!childFile.canRead()) {
                        return false;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    if (childFile.isHidden()) {
                        return false;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                if (childFile.isFile()) {
                    return false;
                } else if (childFile.isDirectory()) {
                    // 主資料夾裡的子資料夾

                    final String[] nameArr = childFile.getName().split("_");

                    if (nameArr.length < 3) {
                        return false;
                    }
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            Timber.e(e);
        }

        if (fileListTArr == null) {
            return 0;
        }

        int serial_number=0;
        for (File file :fileListTArr) {
            final String[] nameArr = file.getName().split("_");

            if(nameArr.length>=3) {
                final String tempID = nameArr[1];
                try {
                    final int i = Integer.parseInt(tempID);
                    if(i>serial_number){
                        serial_number=i;
                    }
                }catch (Exception exception){
                    //Timber.w("");
                }

            }
        }
        return serial_number;
    }
}
