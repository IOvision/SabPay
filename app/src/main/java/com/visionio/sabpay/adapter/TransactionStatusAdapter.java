package com.visionio.sabpay.adapter;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.R;

import java.util.List;
import java.util.Map;

public class TransactionStatusAdapter extends RecyclerView.Adapter<TransactionStatusAdapter.TransactionStatusViewHolder> {

    List<Map<String, String>> transactionStatus;

    public TransactionStatusAdapter(List<Map<String, String>> transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    @NonNull
    @Override
    public TransactionStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TransactionStatusViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_status_list_item, parent, false));
    }

    public void add(Map<String, String> object){
        transactionStatus.add(object);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionStatusViewHolder holder, int position) {
        Map<String, String> curr = transactionStatus.get(position);
        holder.id.setText(curr.get("id"));
        holder.time.setText(curr.get("time"));
        holder.user.setText(curr.get("user"));
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                holder.progressBar.setBackgroundResource(R.drawable.ic_confirm);
            }
        }, 2000);
    }

    @Override
    public int getItemCount() {
        return transactionStatus.size();
    }

    public class TransactionStatusViewHolder extends RecyclerView.ViewHolder{

        TextView id;
        TextView time;
        TextView user;

        ProgressBar progressBar;

        public TransactionStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.transaction_id);
            time = itemView.findViewById(R.id.transaction_time);
            user = itemView.findViewById(R.id.transaction_user);
            progressBar = itemView.findViewById(R.id.transaction_progress);
        }
    }
}
