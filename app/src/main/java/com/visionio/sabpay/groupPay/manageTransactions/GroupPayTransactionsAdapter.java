package com.visionio.sabpay.groupPay.manageTransactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.R;

import java.util.List;

public class GroupPayTransactionsAdapter extends RecyclerView.Adapter<GroupPayTransactionsAdapter.GroupPayTransactionsViewHolder>{

    List<Transaction> transactions;

    public GroupPayTransactionsAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public GroupPayTransactionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupPayTransactionsViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.group_pay_transaction_list_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull GroupPayTransactionsViewHolder holder, int position) {
        Transaction current = transactions.get(position);
        holder.name.setText(current.getFrom().getId());
        holder.date.setText(current.getTimestamp().toDate().toString());
        holder.amount.setText(current.getAmount().toString());


        holder.amount.setText("+ Rs. "+current.getAmount());
        holder.name.setText(current.getDescription());

    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void add(Transaction transaction){
        transactions.add(transaction);
        notifyDataSetChanged();
    }

    public class GroupPayTransactionsViewHolder extends RecyclerView.ViewHolder{

        TextView name;
        TextView date;
        TextView amount;
        View view;

        public GroupPayTransactionsViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            name = view.findViewById(R.id.gPay_transaction_list_item_description_tv);
            date = view.findViewById(R.id.gPay_transaction_list_item_dateTime_tv);
            amount = view.findViewById(R.id.gPay_transaction_list_item_amount_tv);



        }
    }

}
