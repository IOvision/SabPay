package com.visionio.sabpay.group_pay.manageTransactions;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.models.GroupPay;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Utils;

import java.util.List;

public class GroupPayAdapter extends RecyclerView.Adapter<GroupPayAdapter.GroupPayViewHolder>{

    Context context;
    List<GroupPay> groupPayList;
    int currentPosition = 0;

    OnItemClickListener<GroupPay> longClickListener;

    RelativeLayout expandedView;

    public GroupPayAdapter(Context context, List<GroupPay> groupPayList, OnItemClickListener<GroupPay> longClickListener) {
        this.context = context;
        this.groupPayList = groupPayList;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public GroupPayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_pay_list_item, parent, false);
        return new GroupPayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupPayViewHolder holder, final int position) {
        final GroupPay  current = groupPayList.get(position);
        holder.date.setText(Utils.getDate(current.getTimestamp()));
        holder.amount.setText(String.format("Rs. %s",current.getAmount().toString()));
        holder.parts.setText(current.getParts().toString());

        if(!current.getActive()){
            holder.mainContainer.setBackgroundColor(Color.RED);
        }

        holder.view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                longClickListener.onItemClicked(current, position, v);
                return true;
            }
        });

        holder.view.setOnClickListener(v -> {

            if(holder.transactionsContainer.getVisibility() == View.VISIBLE){
                holder.transactionsContainer.setVisibility(View.GONE);
                expandedView = null;
            }else{
                if(expandedView!=null){
                    expandedView.setVisibility(View.GONE);
                }
                current.setRecyclerView(holder.recyclerView);
                current.loadTransaction(context, holder.progressBar);
                holder.transactionsContainer.setVisibility(View.VISIBLE);
                expandedView = holder.transactionsContainer;
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

        TextView date;
        TextView amount;
        TextView parts;
        RecyclerView recyclerView;
        ProgressBar progressBar;
        View view;

        RelativeLayout mainContainer;
        RelativeLayout transactionsContainer;

        public GroupPayViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            date = view.findViewById(R.id.gPay_item_date_tv);
            amount = view.findViewById(R.id.gPay_item_amount_tv);
            parts = view.findViewById(R.id.gPay_item_parts_tv);
            progressBar = view.findViewById(R.id.gPay_item_transactionProgress_pb);
            recyclerView = view.findViewById(R.id.gPay_item_parts_rv);
            mainContainer = view.findViewById(R.id.gPay_item_mainContainer_rl);
            transactionsContainer = view.findViewById(R.id.gPay_item_transactionContainer_ll);
        }
    }
}
