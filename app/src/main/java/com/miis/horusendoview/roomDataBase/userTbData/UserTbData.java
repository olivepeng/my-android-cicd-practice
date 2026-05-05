package com.miis.horusendoview.roomDataBase.userTbData;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.miis.horusendoview.type.UserRoleType;

import java.util.Objects;

@Entity
public class UserTbData {

    @NonNull
    private String uid = "";

    @PrimaryKey
    @NonNull
    private String account = "";

    @NonNull
    private String password = "";

    @NonNull
    private String role = "";

    public UserTbData() {

    }

    @Ignore
    public UserTbData(
            @NonNull String uid,
            @NonNull String account,
            @NonNull String password,
            @NonNull String role) {
        this.uid = uid;
        this.account = account;
        this.password = password;
        this.role = role;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    @NonNull
    public String getAccount() {
        return account;
    }

    public void setAccount(@NonNull String account) {
        this.account = account;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public void setPassword(@NonNull String password) {
        this.password = password;
    }

    @NonNull
    public String getRole() {
        return role;
    }

    public void setRole(@NonNull String role) {
        this.role = role;
    }

    @NonNull
    public UserRoleType getRoleType() {
        @NonNull UserRoleType type = UserRoleType.GUEST;
        for (UserRoleType t : UserRoleType.values()) {
            if (t.name().equals(role)) {
                type = t;
                break;
            }
        }
        return type;
    }

    public void setRoleType(@NonNull UserRoleType type) {
        role = type.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTbData that = (UserTbData) o;
        return Objects.equals(uid, that.uid) &&
                Objects.equals(account, that.account) &&
                Objects.equals(password, that.password) &&
                Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, account, password, role);
    }

    @Override
    public String toString() {
        return "UserTbData{" +
                "uid='" + uid + '\'' +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
