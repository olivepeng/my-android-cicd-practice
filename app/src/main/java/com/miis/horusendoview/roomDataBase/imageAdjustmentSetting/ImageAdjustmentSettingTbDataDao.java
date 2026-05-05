package com.miis.horusendoview.roomDataBase.imageAdjustmentSetting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.libuvccamera.usb.Size;

import java.util.List;

@Dao
public interface ImageAdjustmentSettingTbDataDao {

    @Nullable
    @Query("SELECT * FROM ImageAdjustmentSettingTbData")
    List<ImageAdjustmentSettingTbData> getAll();

    @Nullable
    @Query("SELECT * FROM ImageAdjustmentSettingTbData WHERE account = :account")
    List<ImageAdjustmentSettingTbData> findDataList(@NonNull String account);

    @Nullable
    @Query("SELECT * FROM ImageAdjustmentSettingTbData WHERE vendorId = :vendorId AND account = :account AND size = :sizeJson LIMIT 1")
    ImageAdjustmentSettingTbData find(@NonNull String vendorId, @NonNull String account, @NonNull Size sizeJson);

    @Insert
    void insert(@NonNull ImageAdjustmentSettingTbData data);

    @Insert
    void insertAll(@NonNull List<ImageAdjustmentSettingTbData> list);

    @Update
    void update(@NonNull ImageAdjustmentSettingTbData data);

    @Update
    void updates(@NonNull List<ImageAdjustmentSettingTbData> list);

    @Delete
    void delete(@NonNull ImageAdjustmentSettingTbData data);

    @Delete
    void deletes(@NonNull List<ImageAdjustmentSettingTbData> list);

    @Query("DELETE FROM ImageAdjustmentSettingTbData")
    void deleteTable();
}

