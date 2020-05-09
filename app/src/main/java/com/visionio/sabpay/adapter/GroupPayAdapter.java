package com.visionio.sabpay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.Models.GroupPay;
import com.visionio.sabpay.R;

import java.util.List;

public class GroupPayAdapter extends RecyclerView.Adapter<GroupPayAdapter.GroupPayViewHolder>{

    Context context;
    List<GroupPay> groupPayList;
    int currentPosition = 0;

    public GroupPayAdapter(Context context, List<GroupPay> groupPayList) {
        this.context = context;
        this.groupPayList = groupPayList;
    }

    @NonNull
    @Override
    public GroupPayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_pay_list_item, parent, false);
        return new GroupPayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupPayViewHolder holder, int position) {
        final GroupPay  current = groupPayList.get(position);
        holder.id.setText(current.getId());
        holder.amount.setText(current.getAmount().toString());
        holder.parts.setText(current.getParts().toString());

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.recyclerView.getVisibility() == View.VISIBLE){
                    holder.recyclerView.setVisibility(View.GONE);
                }
                current.setRecyclerView(holder.recyclerView);
                current.loadTransaction(context);
                holder.recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupPayList.size();
    }

    public void add(GroupPay groupPay){
        groupPayList.add(groupPay);
        notifyItemInserted(currentPosition++);
    }

    public class GroupPayViewHolder extends RecyclerView.ViewHolder{

        TextView id;
        TextView amount;
        TextView parts;
        RecyclerView recyclerView;
        View view;

        public GroupPayViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            id = view.findViewById(R.id.gPay_item_id_tv);
            amount = view.findViewById(R.id.gPay_item_amount_tv);
            parts = view.findViewById(R.id.gPay_item_parts_tv);
            recyclerView = view.findViewById(R.id.gPay_item_parts_rv);
        }

    }

}
