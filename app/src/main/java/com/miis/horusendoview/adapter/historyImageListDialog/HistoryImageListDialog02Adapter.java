package com.miis.horusendoview.adapter.historyImageListDialog;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.PopupWindowCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.data.HistoryImageListDialog02AdapterData;
import com.miis.horusendoview.data.HistoryImageListDialogAdapterData;
import com.miis.horusendoview.databinding.AdapterHistoryImageListDialog02Binding;
import com.miis.horusendoview.databinding.DialogHistoryImageListBinding;
import com.miis.horusendoview.dialog.HistoryImageListDialog;
import com.miis.horusendoview.tools.UiTool;

import java.util.List;

import timber.log.Timber;

public class HistoryImageListDialog02Adapter extends ListAdapter<HistoryImageListDialog02AdapterData, HistoryImageListDialog02Adapter.ViewHolder> {
    private final HistoryImageListDialog dialog;

    public HistoryImageListDialog02Adapter(@NonNull HistoryImageListDialog dialog) {
        super(new DiffUtil.ItemCallback<HistoryImageListDialog02AdapterData>() {
            @Override
            public boolean areItemsTheSame(@NonNull HistoryImageListDialog02AdapterData oldItem, @NonNull HistoryImageListDialog02AdapterData newItem) {
                return oldItem.getFile().getName().equals(newItem.getFile().getName());
            }

            @Override
            public boolean areContentsTheSame(@NonNull HistoryImageListDialog02AdapterData oldItem, @NonNull HistoryImageListDialog02AdapterData newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.dialog = dialog;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterHistoryImageListDialog02Binding binding = AdapterHistoryImageListDialog02Binding.inflate(
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

    public class ViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        public final AdapterHistoryImageListDialog02Binding binding;

        @Nullable
        private PopupWindow popupWindow;

        public ViewHolder(@NonNull AdapterHistoryImageListDialog02Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setBind(int position) {
            HistoryImageListDialog02AdapterData data = null;
            try {
                data = getItem(position);
            } catch (Exception e) {
                Timber.e(e);
            }

            if (data == null) {
                setRecycled();
                return;
            }

            try {
                Glide.with(binding.img).clear(binding.img);
            } catch (Exception e) {
                Timber.e(e);
            }
            Glide.with(binding.img).load(data.getFile()).into(binding.img);

            if (data.isSelect()) {
                binding.img.setStrokeWidth(8f);
                binding.img.setSelected(true);
            } else {
                binding.img.setStrokeWidth(0f);
                binding.img.setSelected(false);
            }

            binding.mainLayout.setOnClickListener(view -> {
                if (popupWindow != null) {
                    popupWindow.dismiss();
                }

                HistoryImageListDialog02AdapterData d = null;
                try {
                    d = getItem(getBindingAdapterPosition());
                } catch (Exception e) {
                    Timber.e(e);
                }

                if (d == null) {
                    return;
                }

                if (dialog.getSelectedData() == d) {
                    return;
                }

                if (dialog.getSelectedData() != null) {
                    dialog.getSelectedData().setSelect(false);

                    try {
                        DialogHistoryImageListBinding dialogBinding = dialog.getBinding();
                        RecyclerView.Adapter<?> adapter01 = null;
                        if (dialogBinding != null) {
                            adapter01 = dialogBinding.recyclerView.getAdapter();
                        }
                        int index01 = -1;
                        int index02 = -1;

                        if (adapter01 instanceof HistoryImageListDialog01Adapter) {
                            HistoryImageListDialog01Adapter adapter = (HistoryImageListDialog01Adapter) adapter01;
                            List<HistoryImageListDialogAdapterData> currentList01 = adapter.getCurrentList();

                            for (int i01 = 0; i01 < currentList01.size(); i01++) {
                                List<HistoryImageListDialog02AdapterData> fileList = currentList01.get(i01).getFileList();
                                for (int i02 = 0; i02 < fileList.size(); i02++) {
                                    if (fileList.get(i02).getFile().getName().equals(dialog.getSelectedData().getFile().getName())) {
                                        index01 = i01;
                                        index02 = i02;
                                    }
                                    if (index02 != -1) {
                                        break;
                                    }
                                }
                                if (index01 != -1) {
                                    break;
                                }
                            }

                            if (index01 != -1 && index02 != -1) {
                                RecyclerView.ViewHolder viewHolder01 = null;
                                try {
                                    viewHolder01 = dialog.getBinding().recyclerView.findViewHolderForAdapterPosition(index01);
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                HistoryImageListDialog02Adapter.ViewHolder viewHolder02 = null;
                                if (viewHolder01 instanceof HistoryImageListDialog01Adapter.ViewHolder) {
                                    try {
                                        viewHolder02 = (ViewHolder) ((HistoryImageListDialog01Adapter.ViewHolder) viewHolder01).binding.recyclerView.findViewHolderForAdapterPosition(index02);
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }
                                }

                                if (viewHolder02 != null) {
                                    RecyclerView.Adapter<? extends RecyclerView.ViewHolder> bindingAdapter = viewHolder02.getBindingAdapter();
                                    if (bindingAdapter != null) {
                                        bindingAdapter.notifyItemChanged(viewHolder02.getBindingAdapterPosition());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }

                d.setSelect(true);

                notifyItemChanged(getBindingAdapterPosition());

                dialog.setSelectedData(d);
            });

            binding.mainLayout.setOnLongClickListener(view -> {
                HistoryImageListDialog02AdapterData d = null;
                try {
                    d = getItem(getBindingAdapterPosition());
                } catch (Exception e) {
                    Timber.e(e);
                }

                if (d == null) {
                    return true;
                }

                if (popupWindow != null) {
                    popupWindow.dismiss();
                }

                AppCompatTextView textView = new AppCompatTextView(binding.mainLayout.getContext()) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent event) {
                        MyApplication myApplication = (MyApplication) getContext().getApplicationContext();
                        MainActivity mainActivity = myApplication.getMainActivity();
                        if (mainActivity != null) {
                            mainActivity.restartStandbyNotificationTimeServiceTimer();
                        }
                        return super.dispatchTouchEvent(event);
                    }
                };
                textView.setTextColor(ContextCompat.getColor(binding.mainLayout.getContext(), R.color.black));
                textView.setTextSize(20f);
                textView.setBackgroundResource(R.color.yellow_f5d60d);
                textView.setText(d.getFile().getName());
                int paddingSize = binding.mainLayout.getContext().getResources().getDimensionPixelSize(R.dimen.padding_4);
                textView.setPadding(paddingSize, paddingSize, paddingSize, paddingSize);
                textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setOnClickListener(view1 -> {
                    if (popupWindow != null) {
                        popupWindow.dismiss();
                    }
                });

                PopupWindow w = new PopupWindow();
                w.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                w.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                w.setOutsideTouchable(true);
                w.setContentView(textView);
                w.setOnDismissListener(() -> binding.cardView.setCardElevation(0f));

                PopupWindowCompat.showAsDropDown(
                        w,
                        binding.img,
                        0,
                        -(binding.getRoot().getMeasuredHeight() - binding.getRoot().getResources().getDimensionPixelSize(R.dimen.margin_16)),
                        Gravity.CENTER
                );

                popupWindow = w;

                binding.cardView.setCardElevation(UiTool.convertDpToPixel(20f, binding.getRoot().getContext()));
                return true;
            });
        }

        public void setRecycled() {
            try {
                Glide.with(binding.img).clear(binding.img);
            } catch (Exception e) {
                Timber.e(e);
            }
            binding.img.setImageDrawable(null);
            if (popupWindow != null) {
                popupWindow.dismiss();
            }
            binding.img.setStrokeWidth(0f);
            binding.img.setSelected(false);
        }
    }
}

