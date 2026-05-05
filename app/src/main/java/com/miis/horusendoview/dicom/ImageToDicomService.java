package com.miis.horusendoview.dicom;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.UIDUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import timber.log.Timber;

public class ImageToDicomService {
    private static final ElementDictionary DICT = ElementDictionary.getStandardElementDictionary();
    private static final int[] TYPE2_TAGS ={
            Tag.ContentDate,
            Tag.ContentTime
    };
    private Attributes staticMetaData = new Attributes();


    private void convertImage2JPEG(String srcPath,String desPath)throws Exception{
        Bitmap bmp = BitmapFactory.decodeFile(srcPath);
        FileOutputStream out = new FileOutputStream(desPath);
        bmp.compress(Bitmap.CompressFormat.JPEG,100,out);
        out.close();
    }

    /**
     *
     * Call the Jpg2DcmUtil to convert it.
     *
     * @param srcPath The JPG filepath.
     * @param desPath The DICOM filepath.
     *
     */
    public synchronized void convertJpg2Dcm(String srcPath, String desPath)throws Exception  {

        String fileName = new File(srcPath).getName();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = fileName.substring(dotIndex + 1);
        Path src = null;
        Path dest = Paths.get(desPath);
        if(extension.equals(".png")){
            String desExtension = srcPath.replace(".png",".jpg");
            convertImage2JPEG(srcPath,desExtension );
            src = Paths.get(desExtension);
        }else{
            src = Paths.get(srcPath);
        }
        Jpg2DcmUtil.convert(src,dest,staticMetaData);
    }


    /**
     *
     * Set up the DICOM metadata.
     * @param PatientName 病患姓名
     * @param PatientID 病患ID
     * @param PatientSex 病患性別
     * @param PatientAge 病患年齡
     * @param PatientBirthDate 病患生日
     * @param StudyDate Study日期
     * @param StudyTime Study時間
     * @param StudyDescription Study描述
     * @param StudyID StudyID
     * @param SeriesDate Series日期
     * @param SeriesTime Series時間
     * @param InstanceNumber InstanceNumber
     * @param AccessionNumber AccessionNumber
     *
     */
    public void initMetaData(String PatientName, String PatientID, String PatientSex, String PatientAge, String PatientBirthDate, String StudyDate,String StudyTime, String StudyDescription, String StudyID, String SeriesDate, String SeriesTime, String SeriesNumber, String InstanceNumber, String AccessionNumber){
        //set UID
        staticMetaData.setString(Tag.StudyInstanceUID,VR.UI, UIDUtils.createUID());
        staticMetaData.setString(Tag.SeriesInstanceUID,VR.UI,UIDUtils.createUID());
        staticMetaData.setString(Tag.SOPInstanceUID,VR.UI,UIDUtils.createUID());

        //patient info
        setMetaData(staticMetaData,Tag.PatientName,PatientName);
        setMetaData(staticMetaData,Tag.PatientID,PatientID);
        setMetaData(staticMetaData,Tag.PatientSex,PatientSex);
        setMetaData(staticMetaData,Tag.PatientAge,PatientAge);
        setMetaData(staticMetaData,Tag.PatientBirthDate,PatientBirthDate);

        //study info
        setMetaData(staticMetaData,Tag.StudyDate,StudyDate);
        setMetaData(staticMetaData,Tag.StudyTime,StudyTime);
        setMetaData(staticMetaData,Tag.StudyDescription,StudyDescription);
        setMetaData(staticMetaData,Tag.StudyID,StudyID);

        //series info
        setMetaData(staticMetaData,Tag.SeriesDate,SeriesDate);
        setMetaData(staticMetaData,Tag.SeriesTime,SeriesTime);


        //number info
        setMetaData(staticMetaData,Tag.SeriesNumber,SeriesNumber);
        setMetaData(staticMetaData,Tag.InstanceNumber,InstanceNumber);
        setMetaData(staticMetaData,Tag.AccessionNumber,AccessionNumber);

        setMetaData(staticMetaData,Tag.SOPClassUID,UID.SecondaryCaptureImageStorage);
        supplementType2Tags(staticMetaData);

    }

    public void setMetaDate(Map<Integer, String> dicomInfo){
        for (Integer key : dicomInfo.keySet()) {
            final String value = dicomInfo.get(key);
            if(value!=null){
                setMetaData(staticMetaData, key, value);
            }
        }
    }

    private void setMetaData(Attributes metaData,int tag,String value){
        if(value!=null){
            metaData.setString(tag,DICT.vrOf(tag),value);
        }
    }

    private void supplementType2Tags(Attributes metaData){
        for(int tag:TYPE2_TAGS)
            if(!metaData.contains(tag))
                metaData.setNull(tag,DICT.vrOf(tag));
    }
}
