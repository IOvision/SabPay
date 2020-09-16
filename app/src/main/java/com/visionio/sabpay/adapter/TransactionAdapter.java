package com.visionio.sabpay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Transaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    List<Transaction> transactions;

    FirebaseAuth mAuth;

    OnItemClickListener<Transaction> listener;
    boolean isOfTypePending=false;
    int position = 0;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
        mAuth = FirebaseAuth.getInstance();
        setHasStableIds(true);
    }

    public TransactionAdapter(List<Transaction> transactions, OnItemClickListener<Transaction> listener, boolean isOfTypePending) {
        this.transactions = transactions;
        this.listener = listener;
        this.isOfTypePending = isOfTypePending;
        mAuth = FirebaseAuth.getInstance();
    }

    public void setTransactionList(List<Transaction> transactionList) {
        if (this.transactions==null) {
            this.transactions = transactionList;
        } else {
            this.transactions.addAll(transactionList);
        }
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
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

    public void onBindViewHolder(@NonNull TransactionViewHolder holder, final int position) {
        final Transaction current = transactions.get(position);


        if (isOfTypePending) {
            holder.pay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClicked(current, position, v);
                }
            });
        } else {
            if (!current.isSendByMe()) {
                holder.symbol.setRotation(180);
                holder.amount.setText("+ \u20B9" + current.getAmount());
            } else {
                holder.amount.setText("- \u20B9" + current.getAmount());
            }
            current.loadUserDataFromReference(this);
        }

        holder.description.setText(current.getDescription());
        holder.dateTime.setText(current.getDate());
    }

    @Override
    public int getItemCount () {
        return transactions.size();
    }

    public void add (Transaction transaction){
        transactions.add(transaction);
        notifyItemInserted(position++);
    }


    public void allClear () {
        transactions.clear();
        position = 0;
        notifyDataSetChanged();
    }

    public class TransactionViewHolder extends RecyclerView.ViewHolder{

        TextView description;
        TextView dateTime;
        ImageView symbol;

        View v;

        TextView amount;
        Button pay;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            symbol = itemView.findViewById(R.id.transaction_list_item_icon_iv);
            description = itemView.findViewById(R.id.transaction_list_item_description_tv);
            dateTime = itemView.findViewById(R.id.transaction_list_item_dateTime_tv);
            symbol = itemView.findViewById(R.id.symbol);

            if(isOfTypePending){
                pay = itemView.findViewById(R.id.transaction_list_item_payButton_tv);
            }else{
                amount = itemView.findViewById(R.id.transaction_list_item_amount_tv);
            }

        }
    }


}