package com.miis.horusendoview.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.miis.horusendoview.data.MySelectStorageDialogData;
import com.miis.horusendoview.databinding.AdapterMySelectStorageDialogBinding;
import com.miis.horusendoview.dialog.MySelectStorageDialog;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MySelectStorageDialogAdapter extends ListAdapter<MySelectStorageDialogData, MySelectStorageDialogAdapter.ViewHolder> {

    private final MySelectStorageDialog dialog;

    public MySelectStorageDialogAdapter(MySelectStorageDialog dialog) {
        super(new DiffUtil.ItemCallback<MySelectStorageDialogData>() {
            @Override
            public boolean areItemsTheSame(@NonNull MySelectStorageDialogData oldItem, @NonNull MySelectStorageDialogData newItem) {
                return oldItem.getText().equals(newItem.getText());
            }

            @Override
            public boolean areContentsTheSame(@NonNull MySelectStorageDialogData oldItem, @NonNull MySelectStorageDialogData newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.dialog = dialog;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                AdapterMySelectStorageDialogBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setBind(position);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.setRecycled();
    }

    @Nullable
    public synchronized MySelectStorageDialogData getSelectedItem() {
        for (MySelectStorageDialogData data : getCurrentList()) {
            if (data.isSelect()) {
                return data;
            }
        }
        return null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterMySelectStorageDialogBinding binding;

        public ViewHolder(AdapterMySelectStorageDialogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setBind(int position) {
            MySelectStorageDialogData data = null;
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
            binding.checkBox.setText(data.getText());
            binding.clickView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MySelectStorageDialogData d = null;
                    try {
                        d = getItem(getBindingAdapterPosition());
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (d == null || d.isSelect()) {
                        return;
                    }

                    final List<MySelectStorageDialogData> currentList = getCurrentList();
                    final ArrayList<MySelectStorageDialogData> list = new ArrayList<>();

                    for (int i = 0; i < currentList.size(); i++) {
                        MySelectStorageDialogData d2 = currentList.get(i);
                        MySelectStorageDialogData newData;
                        if (i == getBindingAdapterPosition()) {
                            newData = new MySelectStorageDialogData(
                                    true,
                                    d2.getText(),
                                    d2.getStorageVolume()
                            );
                        } else {
                            newData = new MySelectStorageDialogData(
                                    false,
                                    d2.getText(),
                                    d2.getStorageVolume()
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
