package com.visionio.sabpay.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.visionio.sabpay.Models.OfflineTransaction;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    List<Transaction> transactions;

    FirebaseAuth mAuth;

    OnItemClickListener<Transaction> listener;
    boolean isOfTypePending=false;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
        mAuth = FirebaseAuth.getInstance();
    }

    public TransactionAdapter(List<Transaction> transactions, OnItemClickListener<Transaction> listener, boolean isOfTypePending) {
        this.transactions = transactions;
        this.listener = listener;
        this.isOfTypePending = isOfTypePending;
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if(isOfTypePending){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_transaction_list_item, parent, false);
        }else{
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_list_item, parent, false);

        }
        return new TransactionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, final int position) {
        final Transaction current = transactions.get(position);


        if(isOfTypePending){
            holder.pay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClicked(current, position, v);
                }
            });
        }else{
            if(!current.isSendByMe()){
                holder.imageView.setRotation(180);
                holder.amount.setTextColor(Color.GREEN);
                holder.amount.setText("+ Rs. "+current.getAmount());
            }else {
                holder.amount.setText("- Rs. "+current.getAmount());
            }
        }

        holder.description.setText(current.getDescription());
        holder.dateTime.setText(current.getDate());

        current.loadUserDataFromReference(this);

    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void add(Transaction transaction){
        transactions.add(transaction);
        notifyDataSetChanged();
    }

    public void allClear(){
        transactions.clear();
        notifyDataSetChanged();
    }

    public class TransactionViewHolder extends RecyclerView.ViewHolder{

        ImageView imageView;
        TextView description;
        TextView dateTime;
        View v;

        TextView amount;
        Button pay;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            imageView = itemView.findViewById(R.id.transaction_list_item_icon_iv);
            description = itemView.findViewById(R.id.transaction_list_item_description_tv);
            dateTime = itemView.findViewById(R.id.transaction_list_item_dateTime_tv);

            if(isOfTypePending){
                pay = itemView.findViewById(R.id.transaction_list_item_payButton_tv);
            }else{
                amount = itemView.findViewById(R.id.transaction_list_item_amount_tv);
            }
        }
    }

}
