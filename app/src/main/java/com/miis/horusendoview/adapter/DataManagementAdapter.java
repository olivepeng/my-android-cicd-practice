package com.miis.horusendoview.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.data.DataManagementAdapterData;
import com.miis.horusendoview.databinding.AdapterDataManagementBinding;
import com.miis.horusendoview.fragment.DataManagementFragment;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class DataManagementAdapter extends ListAdapter<@NotNull DataManagementAdapterData, DataManagementAdapter.ViewHolder> {
    private final DataManagementFragment fragment;

    public DataManagementAdapter(DataManagementFragment fragment) {
        super(new DiffUtil.ItemCallback<DataManagementAdapterData>() {
            @Override
            public boolean areItemsTheSame(@NonNull DataManagementAdapterData oldItem, @NonNull DataManagementAdapterData newItem) {
                if (newItem.getFile() != null) {
                    return newItem.getFile().getName().equals(oldItem.getFile() != null ? oldItem.getFile().getName() : null);
                } else {
                    return newItem.getDirDisplayName() != null && newItem.getDirDisplayName().equals(oldItem.getDirDisplayName());
                }
            }

            @Override
            public boolean areContentsTheSame(@NonNull DataManagementAdapterData oldItem, @NonNull DataManagementAdapterData newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterDataManagementBinding binding = AdapterDataManagementBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
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

    public int previous(){
        int indexHighlightOld=-1;
        for (int i = getItemCount()-1; i >=0 ; i--) {
            boolean isHighlightOld = getItem(i).isHighlight();
            if(isHighlightOld){
                if(i>0) {
                    indexHighlightOld = i;
                    getItem(i).setHighlight(false);
                    notifyItemChanged(i);
                }
            }else if(indexHighlightOld>0 && i==(indexHighlightOld-1)){
                getItem(i).setHighlight(true);
                notifyItemChanged(i);
                fragment.setPreviewFile(getItem(i).getFile());
                return i;
            }
        }
        return 0;
    }

    public int next(){
        int indexHighlightOld=-1;
        for (int i = 0; i <getItemCount() ; i++) {
            boolean isHighlightOld = getItem(i).isHighlight();
            if(isHighlightOld){
                if(i < (getItemCount()-1)) {
                    indexHighlightOld = i;
                    getItem(i).setHighlight(false);
                    notifyItemChanged(i);
                }
            }else if(indexHighlightOld>=0 && i==(indexHighlightOld+1)){
                getItem(i).setHighlight(true);
                notifyItemChanged(i);
                fragment.setPreviewFile(getItem(i).getFile());
                return i;
            }
        }
        return getItemCount()-1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final AdapterDataManagementBinding binding;

        public ViewHolder(AdapterDataManagementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setBind(int position) {
            DataManagementAdapterData data = null;
            try {
                data = getItem(position);
            } catch (Exception e) {
                Timber.e(e);
            }

            if (data == null) {
                setRecycled();
                return;
            }

            binding.checkBox.setChecked(data.isSelect());

            if (data.isHighlight()) {
                binding.mainLayout.setBackgroundResource(R.color.blue_2c61c8);
            } else {
                binding.mainLayout.setBackgroundColor(Color.TRANSPARENT);
            }

            if (data.getDirDisplayName() != null && !data.getDirDisplayName().isEmpty()) {
                binding.icon.setImageResource(R.drawable.folder_y);
            } else if (data.getFile() != null && data.getFile().getName().contains(MyApplication.VIDEO_FILE_EXTENSION)) {
                binding.icon.setImageResource(R.drawable.video_file);
            } else {
                binding.icon.setImageResource(R.drawable.image_file);
            }

            binding.checkBoxClick.setOnClickListener(v -> {
                DataManagementAdapterData d = null;
                try {
                    d = getItem(getBindingAdapterPosition());
                } catch (Exception e) {
                    Timber.e(e);
                }

                if (d == null) {
                    return;
                }

                d.setSelect(!d.isSelect());

                notifyItemChanged(getBindingAdapterPosition());

                fragment.checkSelectAll();
            });

            if (data.getDirDisplayName() != null && !data.getDirDisplayName().isEmpty()) {
                binding.name.setText(data.getDirDisplayName());

                DataManagementAdapterData finalData = data;
                GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(binding.getRoot().getContext(), new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        for (int i = 0; i < getItemCount(); i++) {
                            boolean isNotifyItemChanged = false;
                            boolean isHighlightOld = getItem(i).isHighlight();
                            getItem(i).setHighlight(i == getBindingAdapterPosition());
                            if (isHighlightOld != getItem(i).isHighlight()) {
                                isNotifyItemChanged = true;
                            }
                            if (isNotifyItemChanged) {
                                notifyItemChanged(i);
                            }
                        }
                        fragment.checkSelectAll();
                        fragment.setProcedureFolderTbDataToView();
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        fragment.setChildDirDisplayName(finalData.getDirDisplayName());
                        return true;
                    }
                });
                binding.mainLayout.setOnTouchListener((v, event) -> {
                    gestureDetectorCompat.onTouchEvent(event);
                    return true;
                });
            } else {
                binding.name.setText(data.getFile() != null ? data.getFile().getName() : "");
                binding.mainLayout.setOnClickListener(v -> {
                    for (int i = 0; i < getItemCount(); i++) {
                        boolean isNotifyItemChanged = false;
                        if (i == getBindingAdapterPosition()) {
                            fragment.setPreviewFile(getItem(i).getFile());
                        }
                        boolean isHighlightOld = getItem(i).isHighlight();
                        getItem(i).setHighlight(i == getBindingAdapterPosition());
                        if (isHighlightOld != getItem(i).isHighlight()) {
                            isNotifyItemChanged = true;
                        }
                        if (isNotifyItemChanged) {
                            notifyItemChanged(i);
                        }
                    }
                    fragment.setProcedureFolderTbDataToView();
                    fragment.checkSelectAll();
                });
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setRecycled() {
            binding.checkBox.setChecked(false);
            binding.icon.setImageResource(R.drawable.image_file);
            binding.name.setText("");
            binding.mainLayout.setOnTouchListener(null);
            binding.mainLayout.setOnClickListener(null);
            binding.checkBoxClick.setOnClickListener(null);
        }
    }
}

