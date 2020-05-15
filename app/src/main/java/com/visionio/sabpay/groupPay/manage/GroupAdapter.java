package com.visionio.sabpay.groupPay.manage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    Context context;
    List<Group> groupList;
    OnItemClickListener<Group> onItemClickListener;

    public GroupAdapter(Context context, List<Group> groupList, OnItemClickListener<Group> onItemClickListener) {
        this.groupList = groupList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, final int position) {
        Group curr = groupList.get(position);
        holder.size.setText(""+curr.getSize());
        holder.name.setText(curr.getName());

        holder.setClickListener(curr, position);

        curr.setMembersListView(holder.membersList);
        curr.setUpMembersList(context);

        if(curr.isMeAdmin){
            holder.edit.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public void addGroup(Group group){
        if(!groupList.contains(group)){
            groupList.add(group);
            notifyDataSetChanged();
        }

    }

    public void addAllGroup(List<Group> groups){
        if(groupList.size()==groups.size()){
            return;
        }
        groupList.addAll(groups);
        notifyDataSetChanged();
    }

    public class GroupViewHolder extends RecyclerView.ViewHolder{

        TextView size;
        TextView name;
        ImageButton edit;
        RecyclerView membersList;
        View v;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            size = itemView.findViewById(R.id.group_list_item_sizeValue_tv);
            name = itemView.findViewById(R.id.group_list_item_groupName_tv);
            edit = itemView.findViewById(R.id.group_list_item_editButton_ib);
            membersList = itemView.findViewById(R.id.group_list_item_membersList_rv);
        }

        public void setClickListener(final Group group, final int pos){
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClicked(group, pos, v);
                }
            });

        }
    }

}
