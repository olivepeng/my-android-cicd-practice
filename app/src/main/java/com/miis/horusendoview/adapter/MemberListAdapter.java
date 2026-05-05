package com.miis.horusendoview.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.data.MemberAdapterData;
import com.miis.horusendoview.databinding.AdapterMemberListBinding;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.fragment.member.MemberListFragment;
import com.miis.horusendoview.fragment.member.MemberUserAdminFragment;
import com.miis.horusendoview.manager.ChangeFragmentManager;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class MemberListAdapter extends ListAdapter<MemberAdapterData, MemberListAdapter.ViewHolder> {
    private final MemberListFragment fragment;

    Executor executor = Executors.newSingleThreadExecutor();

    public MemberListAdapter(MemberListFragment fragment) {
        super(new DiffUtil.ItemCallback<MemberAdapterData>() {
            @Override
            public boolean areItemsTheSame(@NonNull MemberAdapterData oldItem, @NonNull MemberAdapterData newItem) {
                return oldItem.getUserTbData().getAccount().equals(newItem.getUserTbData().getAccount());
            }

            @Override
            public boolean areContentsTheSame(@NonNull MemberAdapterData oldItem, @NonNull MemberAdapterData newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterMemberListBinding binding = AdapterMemberListBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
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

    public List<UserTbData> getSelectUserTbDataList() {
        List<MemberAdapterData> currentList = getCurrentList();
        List<UserTbData> list = new ArrayList<>();
        final Context context = fragment.getContext();
        MyApplication myApplication = null;
        if (context != null) {
            myApplication = (MyApplication) context.getApplicationContext();
        }

        UserTbData userTbData = null;
        if (myApplication != null) {
            userTbData = myApplication.getLoginUserTbData();
        }

        for (MemberAdapterData d : currentList) {
            if (d.isSelect() &&
                    !d.getUserTbData().getAccount().equalsIgnoreCase("admin") &&
                    !d.getUserTbData().getAccount().equalsIgnoreCase("service") &&
                    !(userTbData != null && d.getUserTbData().getAccount().equals(userTbData.getAccount()))) {
                list.add(d.getUserTbData());
            }
        }

        return list;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final AdapterMemberListBinding binding;

        public ViewHolder(AdapterMemberListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setBind(int position) {
            MemberAdapterData data = null;
            try {
                data = getItem(position);
            } catch (Exception e) {
                Timber.e(e);
            }

            if (data == null) {
                setRecycled();
                return;
            }

            final Context context = fragment.getContext();

            MyApplication myApplication = null;
            if (context != null) {
                myApplication = (MyApplication) context.getApplicationContext();
            }
            setRecycled();
            if (myApplication == null) {
                return;
            }

            binding.checkBox.setChecked(data.isSelect());

            binding.mainLayout.setSelected(data.isSelect());

            binding.name.setSelected(data.isSelect());

            binding.name.setText(data.getUserTbData().getAccount());

            //if (data.getUserTbData().getRoleType() != null) {
                switch (data.getUserTbData().getRoleType()) {
                    case GUEST:
                        binding.roleImg.setImageResource(R.drawable.default_user_128x128);
                        binding.roleText.setText("-");
                        break;
                    case ADVANCED_USER:
                        binding.roleImg.setImageResource(R.drawable.default_user_128x128);
                        binding.roleText.setText(R.string.advanced_user);
                        break;
                    case ADMIN_USER:
                        binding.roleImg.setImageResource(R.drawable.admin_user_128x128);
                        binding.roleText.setText(R.string.admin_user);
                        break;
                    case SERVICE_USER:
                        binding.roleImg.setImageResource(R.drawable.admin_user_128x128);
                        binding.roleText.setText(R.string.service_user);
                        break;
                }
//            } else {
//                binding.roleImg.setImageResource(R.drawable.default_user_128x128);
//                binding.roleText.setText("-");
//            }


            binding.mainLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MemberAdapterData d = null;
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
                }
            });

            binding.edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MemberAdapterData d = null;
                    try {
                        d = getItem(getBindingAdapterPosition());
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    if (d == null) {
                        return;
                    }
                    MemberUserAdminFragment memberUserAdminFragment = MemberUserAdminFragment.newInstance(d.getUserTbData());
                    ChangeFragmentManager changeChildFragmentManager = fragment.getChangeChildFragmentManager();
                    if (changeChildFragmentManager != null) {
                        changeChildFragmentManager.changePage(memberUserAdminFragment);
                    }
                }
            });

            UserTbData userTbData = myApplication.getLoginUserTbData();
            if (data.getUserTbData().getAccount().equalsIgnoreCase("admin") ||
                    data.getUserTbData().getAccount().equalsIgnoreCase("service") ||
                    (userTbData != null && data.getUserTbData().getAccount().equals(userTbData.getAccount()))) {
                binding.delete.setVisibility(View.INVISIBLE);
            } else {
                binding.delete.setVisibility(View.VISIBLE);
            }

            binding.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MemberAdapterData d = null;
                    try {
                        d = getItem(getBindingAdapterPosition());
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    if (d == null) {
                        return;
                    }

                    if (d.getUserTbData().getAccount().equals("admin")) {
                        return;
                    }

                    MemberAdapterData finalD = d;
                    fragment.showDeleteUserDialog(new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            try {
                                executor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        fragment.deleteUser(finalD.getUserTbData());
                                        fragment.setDataToRecyclerView();
                                    }
                                });
                            } catch (Exception e) {
                               Timber.e(e);
                            }
                        }

                        @Override
                        public void OnClickCancel() {

                        }
                    });

                }
            });
        }

        public void setRecycled() {
            binding.mainLayout.setOnClickListener(null);
            binding.checkBox.setChecked(false);
            binding.name.setText("");
            binding.roleImg.setImageDrawable(null);
            binding.roleText.setText("");
            binding.edit.setOnClickListener(null);
            binding.delete.setOnClickListener(null);
            binding.delete.setVisibility(View.VISIBLE);
        }
    }
}

