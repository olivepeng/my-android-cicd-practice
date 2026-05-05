package com.miis.horusendoview.roomDataBase.userTbData;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;


/**
 * 登入會員資料
 */
@Dao
public interface UserTbDataDao {

    @Nullable
    @Query("SELECT * FROM UserTbData")
    List<UserTbData> getAll();

    @Nullable
    @Query("SELECT * FROM UserTbData WHERE LOWER(account) = :account LIMIT 1")
    UserTbData findByAccount(@NonNull String account);

    @Nullable
    @Query("SELECT * FROM UserTbData WHERE LOWER(account) = :account AND password = :password LIMIT 1")
    UserTbData findByAccountAndPassword(@NonNull String account, @NonNull String password);

    @Insert
    void insert(@NonNull UserTbData data);

    @Insert
    void insertAll(@NonNull List<UserTbData> list);

    @Update
    void update(@NonNull UserTbData data);

    @Update
    void updates(@NonNull List<UserTbData> list);

    @Delete
    void delete(@NonNull UserTbData data);

    @Delete
    void deletes(@NonNull List<UserTbData> list);

    @Query("DELETE FROM UserTbData")
    void deleteTable();
}

