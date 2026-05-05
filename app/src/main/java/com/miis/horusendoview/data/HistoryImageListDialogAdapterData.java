package com.miis.horusendoview.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HistoryImageListDialogAdapterData {

    @NonNull
    private LocalDate date;

    @NonNull
    private List<HistoryImageListDialog02AdapterData> fileList;

    public HistoryImageListDialogAdapterData(@NonNull LocalDate date, @NonNull List<HistoryImageListDialog02AdapterData> fileList) {
        this.date = date;
        this.fileList = fileList;
    }

    public HistoryImageListDialogAdapterData(@NonNull LocalDate date) {
        this.date = date;
        this.fileList = new ArrayList<>(); // You can choose to initialize it as null or an empty list in the constructor.
    }

    @NonNull
    public LocalDate getDate() {
        return date;
    }

    public void setDate(@NonNull LocalDate date) {
        this.date = date;
    }

    @NonNull
    public List<HistoryImageListDialog02AdapterData> getFileList() {
        return fileList;
    }

    public void setFileList(@NonNull List<HistoryImageListDialog02AdapterData> fileList) {
        this.fileList = fileList;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HistoryImageListDialogAdapterData that = (HistoryImageListDialogAdapterData) obj;
        return Objects.equals(date, that.date) &&
                Objects.equals(fileList, that.fileList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, fileList);
    }
}
