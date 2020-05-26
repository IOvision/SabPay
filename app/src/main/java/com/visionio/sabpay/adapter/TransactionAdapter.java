package com.visionio.sabpay.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.visionio.sabpay.Models.OfflineTransaction;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.R;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    List<Transaction> transactions;

    FirebaseAuth mAuth;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_list_item, parent, false);
        return new TransactionViewHolder(v);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction current = transactions.get(position);
        if (!current.isSendByMe()){
            holder.symbol.setRotation(180);
            holder.amount.setText("+ \u20B9"+current.getAmount());
        } else {
            holder.amount.setText("- \u20B9"+current.getAmount());
        }

        holder.description.setText(current.getDescription());
        holder.dateTime.setText(current.getDate());
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void add(Transaction transaction){
        transactions.add(transaction);
        //notifyDataSetChanged();
    }

    public void clear() {
        transactions.clear();
    }

    public class TransactionViewHolder extends RecyclerView.ViewHolder{

        TextView description;
        TextView amount;
        TextView dateTime;
        ImageView symbol;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.transaction_list_item_description_tv);
            amount = itemView.findViewById(R.id.transaction_list_item_amount_tv);
            dateTime = itemView.findViewById(R.id.transaction_list_item_dateTime_tv);
            symbol = itemView.findViewById(R.id.symbol);
        }
    }

}
