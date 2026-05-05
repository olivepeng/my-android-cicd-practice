package com.miis.horusendoview.adapter.historyImageListDialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.miis.horusendoview.data.HistoryImageListDialogAdapterData;
import com.miis.horusendoview.databinding.AdapterHistoryImageListDialog01Binding;
import com.miis.horusendoview.dialog.HistoryImageListDialog;
import com.miis.horusendoview.itemDecoration.GridSpacingItemDecoration;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import timber.log.Timber;

public class HistoryImageListDialog01Adapter extends ListAdapter<HistoryImageListDialogAdapterData, HistoryImageListDialog01Adapter.ViewHolder> {

    private final HistoryImageListDialog dialog;

    public HistoryImageListDialog01Adapter(@NonNull HistoryImageListDialog dialog) {
        super(new DiffUtil.ItemCallback<HistoryImageListDialogAdapterData>() {
            @Override
            public boolean areItemsTheSame(@NonNull HistoryImageListDialogAdapterData oldItem, @NonNull HistoryImageListDialogAdapterData newItem) {
                return oldItem.getDate().isEqual(newItem.getDate());
            }

            @Override
            public boolean areContentsTheSame(@NonNull HistoryImageListDialogAdapterData oldItem, @NonNull HistoryImageListDialogAdapterData newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.dialog = dialog;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterHistoryImageListDialog01Binding binding = AdapterHistoryImageListDialog01Binding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
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

    public class ViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        public final AdapterHistoryImageListDialog01Binding binding;

        public ViewHolder(@NonNull AdapterHistoryImageListDialog01Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setBind(int position) {
            HistoryImageListDialogAdapterData data = null;
            try {
                data = getItem(position);
            } catch (Exception e) {
                Timber.e(e);
            }

            if (data == null) {
                setRecycled();
                return;
            }

            binding.title.setText(data.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE.withLocale(Locale.US)));

            DefaultItemAnimator itemAnimator = (DefaultItemAnimator) binding.recyclerView.getItemAnimator();
            if (itemAnimator != null) {
                itemAnimator.setSupportsChangeAnimations(false);
            }

            if (!(binding.recyclerView.getLayoutManager() instanceof GridLayoutManager)) {
                binding.recyclerView.setLayoutManager(new GridLayoutManager(binding.recyclerView.getContext(), 4) {
                    @Override
                    public boolean canScrollVertically() {
                        return false;
                    }
                });
            }

            if (!(binding.recyclerView.getAdapter() instanceof HistoryImageListDialog02Adapter)) {
                binding.recyclerView.setAdapter(new HistoryImageListDialog02Adapter(dialog));
            }

            if (binding.recyclerView.getItemDecorationCount() == 0) {
                GridSpacingItemDecoration gridSpacingItemDecoration = new GridSpacingItemDecoration(4, 19, false);
                binding.recyclerView.addItemDecoration(gridSpacingItemDecoration);
            }

            RecyclerView.Adapter<?> adapter = binding.recyclerView.getAdapter();
            if (adapter instanceof HistoryImageListDialog02Adapter) {
                ((HistoryImageListDialog02Adapter) adapter).submitList(data.getFileList());
            }
        }

        public void setRecycled() {
            binding.title.setText("");
            binding.recyclerView.setAdapter(null);
            binding.recyclerView.setLayoutManager(null);
        }
    }
}

