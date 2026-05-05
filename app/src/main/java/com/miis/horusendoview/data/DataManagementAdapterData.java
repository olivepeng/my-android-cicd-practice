package com.miis.horusendoview.data;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Objects;

public class DataManagementAdapterData {

    private boolean isSelect = false;

    /**
     * 資料夾顯示時，選取資料夾
     */
    private boolean isHighlight = false;

    @Nullable
    private String dirDisplayName = null;

    @Nullable
    private File file = null;

    public DataManagementAdapterData() {
        // Default constructor
    }

    public DataManagementAdapterData(boolean isSelect, boolean isHighlight, @Nullable String dirDisplayName, @Nullable File file) {
        this.isSelect = isSelect;
        this.isHighlight = isHighlight;
        this.dirDisplayName = dirDisplayName;
        this.file = file;

    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public boolean isHighlight() {
        return isHighlight;
    }

    public void setHighlight(boolean highlight) {
        isHighlight = highlight;
    }

    @Nullable
    public String getDirDisplayName() {
        return dirDisplayName;
    }

    public void setDirDisplayName(@Nullable String dirDisplayName) {
        this.dirDisplayName = dirDisplayName;
    }

    @Nullable
    public File getFile() {
        return file;
    }

    public void setFile(@Nullable File file) {
        this.file = file;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DataManagementAdapterData that = (DataManagementAdapterData) obj;
        return Objects.equals(isSelect, that.isSelect) &&
                Objects.equals(isHighlight, that.isHighlight) &&
                Objects.equals(dirDisplayName, that.dirDisplayName) &&
                Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSelect, isHighlight, dirDisplayName, file);
    }
}
