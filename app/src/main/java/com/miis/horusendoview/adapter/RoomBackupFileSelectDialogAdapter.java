package com.miis.horusendoview.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.miis.horusendoview.data.RoomBackupFileSelectDialogAdapterData;
import com.miis.horusendoview.databinding.AdapterRoomBackupFileSelectDialogBinding;
import com.miis.horusendoview.dialog.RoomBackupFileSelectDialog;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class RoomBackupFileSelectDialogAdapter extends ListAdapter<RoomBackupFileSelectDialogAdapterData, RoomBackupFileSelectDialogAdapter.ViewHolder> {

    @NonNull
    private final RoomBackupFileSelectDialog dialog;

    public RoomBackupFileSelectDialogAdapter(@NonNull RoomBackupFileSelectDialog dialog) {
        super(new DiffUtil.ItemCallback<RoomBackupFileSelectDialogAdapterData>() {
            @Override
            public boolean areItemsTheSame(@NonNull RoomBackupFileSelectDialogAdapterData oldItem, @NonNull RoomBackupFileSelectDialogAdapterData newItem) {
                return oldItem.getFile().equals(newItem.getFile());
            }

            @Override
            public boolean areContentsTheSame(@NonNull RoomBackupFileSelectDialogAdapterData oldItem, @NonNull RoomBackupFileSelectDialogAdapterData newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.dialog = dialog;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterRoomBackupFileSelectDialogBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        ));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setBind(position);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.setRecycled();
    }

    @Nullable
    public synchronized RoomBackupFileSelectDialogAdapterData getSelectedItem() {
        for (RoomBackupFileSelectDialogAdapterData data : getCurrentList()) {
            if (data.isSelect()) {
                return data;
            }
        }
        return null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final AdapterRoomBackupFileSelectDialogBinding binding;

        public ViewHolder(@NonNull AdapterRoomBackupFileSelectDialogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setBind(int position) {
            RoomBackupFileSelectDialogAdapterData data = null;
            try {
                data = getItem(position);
            } catch (Exception e) {
                Timber.e(e);
            }
            if (data == null) {
                setRecycled();
                return;
            }

            binding.checkBox.setOnCheckedChangeListener(null);
            binding.checkBox.setChecked(data.isSelect());
            binding.checkBox.setText(data.getFile().getName());
            binding.clickView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RoomBackupFileSelectDialogAdapterData d = null;
                    try {
                        d = getItem(getBindingAdapterPosition());
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (d == null || d.isSelect()) {
                        return;
                    }

                    final List<RoomBackupFileSelectDialogAdapterData> currentList = getCurrentList();
                    final ArrayList<RoomBackupFileSelectDialogAdapterData> list = new ArrayList<>();

                    for (int i = 0; i < currentList.size(); i++) {
                        RoomBackupFileSelectDialogAdapterData d2 = currentList.get(i);
                        RoomBackupFileSelectDialogAdapterData newData;
                        if (i == getBindingAdapterPosition()) {
                            newData = new RoomBackupFileSelectDialogAdapterData(
                                    true,
                                    d2.getFile()
                            );
                        } else {
                            newData = new RoomBackupFileSelectDialogAdapterData(
                                    false,
                                    d2.getFile()
                            );
                        }
                        list.add(newData);
                    }
                    submitList(list, new Runnable() {
                        @Override
                        public void run() {
                            dialog.checkConfirmBtn();
                        }
                    });
                }
            });
        }

        public void setRecycled() {
            binding.checkBox.setOnCheckedChangeListener(null);
            binding.checkBox.setChecked(false);
            binding.checkBox.setText("");
        }
    }
}
