package com.miis.horusendoview.roomDataBase.procedureFolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProcedureFolderTbDataDao {

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData")
    List<ProcedureFolderTbData> getAll();

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData WHERE account = :account")
    List<ProcedureFolderTbData> findDataList(@NonNull String account);

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData WHERE patientId = :patientId AND createDateTime = :createDate")
    List<ProcedureFolderTbData> findDataList(@NonNull String patientId, @NonNull String createDate);

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData WHERE account = :account AND patientId = :patientId AND createDateTime = :createDate")
    List<ProcedureFolderTbData> findDataList(@NonNull String account, @NonNull String patientId, @NonNull String createDate);

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData WHERE filePath = :dirFilePath LIMIT 1")
    ProcedureFolderTbData find(@NonNull String dirFilePath);

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData WHERE account = :account AND filePath = :dirFilePath LIMIT 1")
    ProcedureFolderTbData find(@NonNull String account, @NonNull String dirFilePath);

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData WHERE account = :account AND patientId = :patientId AND filePath = :dirFilePath AND createDateTime = :createDate LIMIT 1")
    ProcedureFolderTbData find(@NonNull String account, @NonNull String patientId, @NonNull String dirFilePath, String createDate);

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData WHERE patientId LIKE '%' || :patientId || '%' AND filePath = :dirFilePath")
    ProcedureFolderTbData fuzzySearchPatientId(@NonNull String patientId, @NonNull String dirFilePath);

    @Nullable
    @Query("SELECT * FROM ProcedureFolderTbData WHERE patientId LIKE '%' || :patientId || '%' AND filePath = :dirFilePath AND account = :account")
    ProcedureFolderTbData fuzzySearchPatientId(@NonNull String patientId, @NonNull String dirFilePath, @NonNull String account);

    @Insert
    void insert(@NonNull ProcedureFolderTbData data);

    @Insert
    void insertAll(@NonNull List<ProcedureFolderTbData> list);

    @Update
    void update(@NonNull ProcedureFolderTbData data);

    @Update
    void updates(@NonNull List<ProcedureFolderTbData> list);

    @Delete
    void delete(@NonNull ProcedureFolderTbData data);

    @Delete
    void deletes(@NonNull List<ProcedureFolderTbData> list);

    @Query("DELETE FROM ProcedureFolderTbData")
    void deleteTable();
}

