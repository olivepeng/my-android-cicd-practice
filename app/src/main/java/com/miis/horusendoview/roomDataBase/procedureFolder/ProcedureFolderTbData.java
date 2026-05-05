package com.miis.horusendoview.roomDataBase.procedureFolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.miis.horusendoview.type.GenderType;

import java.time.LocalDate;
import java.util.Objects;

import timber.log.Timber;

@Entity(primaryKeys = {"filePath", "account", "createDateTime", "patientId"})
public class ProcedureFolderTbData {

    @NonNull
    private String id = "";

    /**
     * 資料夾路徑
     */
    @NonNull
    private String filePath = "";

    @NonNull
    private String account = "";

    @NonNull
    private String createDateTime = "";

    @NonNull
    private String patientId = "";

    @Nullable
    private String division = null;

    @Nullable
    private String age = null;

    @Nullable
    private String gender = null;

    @Nullable
    private String portion = null;

    @Nullable
    private String remark = null;

    public ProcedureFolderTbData() {

    }

    @Ignore
    public ProcedureFolderTbData(@NonNull String id,
                                 @NonNull String filePath,
                                 @NonNull String account,
                                 @NonNull String patientId) {
        this.id = id;
        this.filePath = filePath;
        this.account = account;
        this.patientId = patientId;

    }

    public void setCreateDate(@NonNull LocalDate localDate) {
        try {
            createDateTime = localDate.toString();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Nullable
    public LocalDate getCreateDate() {
        try {
            return LocalDate.parse(createDateTime);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @Nullable
    public GenderType getGenderType() {
        for (GenderType type : GenderType.values()) {
            if (type.name().equals(gender)) {
                return type;
            }
        }
        return null;
    }


    public void setGenderType(@NonNull GenderType type) {
        gender = type.name();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(@NonNull String filePath) {
        this.filePath = filePath;
    }

    @NonNull
    public String getAccount() {
        return account;
    }

    public void setAccount(@NonNull String account) {
        this.account = account;
    }

    @NonNull
    public String getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(@NonNull String createDateTime) {
        this.createDateTime = createDateTime;
    }

    @NonNull
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(@NonNull String patientId) {
        this.patientId = patientId;
    }

    @Nullable
    public String getDivision() {
        return division;
    }

    public void setDivision(@Nullable String division) {
        this.division = division;
    }

    @Nullable
    public String getAge() {
        return age;
    }

    public void setAge(@Nullable String age) {
        this.age = age;
    }

    @Nullable
    public String getGender() {
        return gender;
    }

    public void setGender(@Nullable String gender) {
        this.gender = gender;
    }

    @Nullable
    public String getPortion() {
        return portion;
    }

    public void setPortion(@Nullable String portion) {
        this.portion = portion;
    }

    @Nullable
    public String getRemark() {
        return remark;
    }

    public void setRemark(@Nullable String remark) {
        this.remark = remark;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcedureFolderTbData that = (ProcedureFolderTbData) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(filePath, that.filePath) &&
                Objects.equals(account, that.account) &&
                Objects.equals(createDateTime, that.createDateTime) &&
                Objects.equals(patientId, that.patientId) &&
                Objects.equals(division, that.division) &&
                Objects.equals(age, that.age) &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(portion, that.portion) &&
                Objects.equals(remark, that.remark)
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filePath, account, createDateTime, patientId, division, age, gender, portion, remark);
    }
}

