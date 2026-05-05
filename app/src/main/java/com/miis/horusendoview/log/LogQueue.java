package com.miis.horusendoview.log;

import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class LogQueue implements Runnable{
    private static final String TAG = "LogQueue";

    public static final File folder = new File(Environment.getExternalStorageDirectory().toString()
            + File.separator + "LOG");

    private File mLogFile;
//    private FileOutputStream mLogOutStream;
//    private Deque<LogVector> mQueue;
    private static LogQueue instance;

    private ArrayList<OldLog> mOldLogList;

    public static LogQueue getInstance() {
        if (instance ==null){
            instance = new LogQueue();
        }
        return instance;
    }

    public LogQueue() {
        mLogFile = createLogFile();
//        String[] title ={"Time","Log Type","Content"};
//        StringBuffer csvText = new StringBuffer();
//        for (int i = 0; i < title.length; i++) {
//            csvText.append(title[i]+",");
//        }
//        csvText.append("\n");
//        try {
//            if(mLogFile!=null) {
//                mLogOutStream = new FileOutputStream(mLogFile);
//                mLogOutStream.write(csvText.toString().getBytes());
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        mQueue=new LinkedList<LogVector>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                removeOldFile(folder, 30);
                removeOldFilesWhenOverSize(1024*1024*1024L);
            }
        }).start();

    }

    private File createLogFile() {

        String datePart = new SimpleDateFormat("yyyy_MM_dd_HHmm", Locale.ENGLISH).format(new Date());
//        String namePart = "test_log_" + datePart + ".csv";


        folder.mkdirs();

//        File file = new File(folder + File.separator + namePart);

//        if (!file.exists()) { //create the file if not exist
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//                Log.e(TAG, "Can not create file: " + file.getName());
//            }
//        }

        String namelog = "log_" + datePart + ".sys";
        try {
            Process process = Runtime.getRuntime().exec("/bin/logcat -d");
            process = Runtime.getRuntime().exec( "logcat -f " + folder + File.separator + namelog);
        }catch(Exception e){
            Log.e(TAG, "Can not create log: " + namelog);
            Timber.w("Exception: "+ e.getMessage());
        }

//        if (file.exists()) {
//            return file;
//        } else {
            return null;
//        }
    }

    private class OldLog{
        File file;
        long size;
    }

    private void removeOldFile(File dir, int keepDays){
        Log.d(TAG,"[removeOldFile]"+dir.getAbsolutePath());
        mOldLogList=new ArrayList<>();

        long currentTime = System.currentTimeMillis();
        try {
            final File[] files = dir.listFiles();
            for (File f:files ) {
                final long filesLastModified = f.lastModified();
                long timeGap = currentTime - filesLastModified;
                timeGap = timeGap/1000/24/3600;
                //Log.d(TAG,"timeGap for file:"+f.getAbsolutePath()+" is "+String.format("%d", timeGap)+" days");
                if(timeGap > keepDays){
                    Log.d(TAG,"delete file:"+f.getAbsolutePath()+" is "+String.format("%d", timeGap)+" days");
                    deleteDir(f);
                }else{ //for removeOldFilesWhenOverSize
                    OldLog mOldLog=new OldLog();
                    mOldLog.file=f;
                    if(f.isDirectory()){
                        mOldLog.size=getFolderSize(f);
                    }else {
                        mOldLog.size=f.length();
                    }
                    mOldLogList.add(mOldLog);
                }
            }
        }catch (Exception e){
            Log.e(TAG,"[removeOldFile] delete file failed:"+e.getMessage());
        }
    }

    public void removeOldFilesWhenOverSize(long max_size){
        if(mOldLogList==null){
            Log.w(TAG,"[removeOldFilesWhenOverSize] == null");
            return;
        }
        long size=0;
        for (OldLog oldLog:mOldLogList) {
            size+=oldLog.size;
        }

        try {
            Log.d(TAG,"[removeOldFilesWhenOverSize] size("+size+") > "+max_size);
            if(size>max_size){
                Collections.sort(mOldLogList, new Comparator<OldLog>() {
                    @Override
                    public int compare(OldLog o1, OldLog o2) {
                        return Long.compare(o1.file.lastModified(), o2.file.lastModified());
                    }
                });

                for (OldLog oldLog:mOldLogList) {
                    final File file = oldLog.file;
                    final long fileSize=oldLog.size;
                    size=size-fileSize;
                    Log.d(TAG,"[removeOldFilesWhenOverSize] delete "+file.getAbsolutePath());
                    deleteDir(file);
                    if(size <= max_size){
                        break;
                    }
                }

            }
        }catch (Exception e){
            Log.e(TAG,"[removeOldFilesWhenOverSize] delete file failed:"+e.getMessage());
        }
    }

    private long getFolderSize(File file){
        long size=0;
        try {
            File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if(fileList[i].isDirectory()){
                    size=size+getFolderSize(fileList[i]);
                }else {
                    size=size+fileList[i].length();
                }
            }
        }catch (Exception e){
            Log.e(TAG,"[getFolderSize] "+e.getMessage());
        }
        return size;
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    @Override
    public void run() {

    }

    /**
     * @return "[package]:id/[xml-id]"
     * where [package] is your package and [xml-id] is id of view
     * or "no-id" if there is no id
     */
    public static String getId(View view) {
        if (view.getId() == View.NO_ID) return "no-id";
            //else return view.getResources().getResourceName(view.getId());
        else return view.getResources().getResourceEntryName(view.getId());
    }
}
