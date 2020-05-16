package com.visionio.sabpay.groupPay.pending;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.Models.Contact;
import com.visionio.sabpay.Models.Utils;
import com.visionio.sabpay.R;
import com.visionio.sabpay.groupPay.manage.Group;
import com.visionio.sabpay.interfaces.OnItemClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GroupSelectorAdapter extends RecyclerView.Adapter<GroupSelectorAdapter.GroupSelectorViewHolder> {

    List<Group> allGroupList;
    List<Group> filteredGroupList;
    OnItemClickListener<Group> listener;

    public GroupSelectorAdapter(List<Group> groupList, OnItemClickListener<Group> listener) {
        this.filteredGroupList = groupList;
        this.allGroupList = new ArrayList<>(groupList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupSelectorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupSelectorViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group_select_list_item, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull GroupSelectorViewHolder holder, int position) {
        Group group = filteredGroupList.get(position);

        holder.name.setText(group.getName());
        holder.size.setText(group.getSize().toString());
        holder.dateCreated.setText("Created: "+Utils.getDate(group.getTimestamp()));

        holder.attachListener(group, position);
    }

    @Override
    public int getItemCount() {
        return filteredGroupList.size();
    }

    public void add(Group group){
        allGroupList.add(group);
        filteredGroupList.add(group);
        notifyDataSetChanged();
    }

    public void applyFilter(String filterString){
        filteredGroupList.clear();
        for(Group c: allGroupList){
            if(c.getName().toLowerCase().contains(filterString)){
                filteredGroupList.add(c);
            }
        }
        notifyDataSetChanged();
    }

    public class GroupSelectorViewHolder extends RecyclerView.ViewHolder{

        TextView size;
        TextView name;
        TextView dateCreated;
        View v;

        public GroupSelectorViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            size = v.findViewById(R.id.group_select_list_item_sizeValue_tv);
            name = v.findViewById(R.id.group_select_list_item_groupName_tv);
            dateCreated = v.findViewById(R.id.group_select_list_item_dateCreated_tv);
        }

        public void attachListener(final Group group, final int pos){
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClicked(group, pos, v);
                }
            });

        }
    }

}
