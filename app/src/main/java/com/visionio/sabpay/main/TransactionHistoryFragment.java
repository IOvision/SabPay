package com.visionio.sabpay.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.visionio.sabpay.InvoiceActivity;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.OrderAdapter;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Order;
import com.visionio.sabpay.models.Transaction;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryFragment extends Fragment {

    public TransactionHistoryFragment() {
        // Required empty public constructor
    }

    Button transactions, orders;
    TextView bg_txt_tv;
    RecyclerView transactionRecyclerView, orderRecyclerView;
    ProgressBar progressBar;
    TransactionAdapter transactionAdapter;
    OrderAdapter orderAdapter;
    MaterialButtonToggleGroup toggleButton;
    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_history, container, false);

        transactionRecyclerView = view.findViewById(R.id.transaction_fragment_recycler);
        orderRecyclerView = view.findViewById(R.id.order_fragment_recycler);
        bg_txt_tv = view.findViewById(R.id.frag_txn_bg_txt_tv);
        progressBar = view.findViewById(R.id.transaction_fragment_pb);
        toggleButton = view.findViewById(R.id.toggleButtonHistory);
        transactions = view.findViewById(R.id.btn_transaction_history);
        orders = view.findViewById(R.id.btn_order_history);
        ((MainActivity)getActivity()).setTitle("History");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toggleButton.setSingleSelection(true);

        toggleButton.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (checkedId == R.id.btn_transaction_history) {
                loadTransactionHistory();
            } else if (checkedId == R.id.btn_order_history) {
                loadOrderHistory();
            } else {
                //
            }
        });
        toggleButton.check(R.id.btn_transaction_history);
    }

    public void loadOrderHistory() {
        transactionRecyclerView.setVisibility(View.GONE);
        orderRecyclerView.setVisibility(View.VISIBLE);
        orderAdapter = new OrderAdapter(new ArrayList<>(), new OnItemClickListener<Order>() {
            @Override
            public void onItemClicked(Order order, int position, View view) {
                Intent i = new Intent(getActivity(), InvoiceActivity.class);
                String orderJson = new Gson().toJson(order);
                i.putExtra("order", orderJson);
                i.putExtra("invoiceId", order.getInvoiceId());
                i.putExtra("orderId", order.getOrderId());
                i.putExtra("orderStatus", order.getStatus());
                startActivity(i);
            }
        });
        orderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderRecyclerView.setHasFixedSize(false);
        orderRecyclerView.setAdapter(orderAdapter);

        loadOrder();
    }


    void loadOrder() {
        progressBar.setVisibility(View.VISIBLE);
        bg_txt_tv.setVisibility(View.GONE);
        FirebaseFirestore.getInstance().collection("order").whereEqualTo("user.userId", FirebaseAuth.getInstance().getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()){
                            if (task.getResult().getDocuments().size() != 0){
                                List<DocumentSnapshot> docs = task.getResult().getDocuments();
                                for (DocumentSnapshot current : docs){
                                    Order order = current.toObject(Order.class);
                                    orderAdapter.add(order);
                                }
                            }else{
                                if(orderRecyclerView.getVisibility()==View.VISIBLE){
                                    bg_txt_tv.setText("No orders yet!");
                                    bg_txt_tv.setVisibility(View.VISIBLE);
                                }
                            }
                        }else{
                            Utils.toast(getContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_LONG);
                        }
                    }
                });
    }

    public void loadTransactionHistory() {
        orderRecyclerView.setVisibility(View.GONE);
        transactionRecyclerView.setVisibility(View.VISIBLE);
        transactionAdapter = new TransactionAdapter(new ArrayList<>());
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionRecyclerView.setHasFixedSize(false);
        transactionRecyclerView.setAdapter(transactionAdapter);

        loadTransactions();
    }

    public void loadTransactions(){
        progressBar.setVisibility(View.VISIBLE);
        bg_txt_tv.setVisibility(View.GONE);
        FirebaseFirestore.getInstance().collection("user")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("transaction")
                // TODO: check the filter thing
                //.whereEqualTo("type", 0)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                progressBar.setVisibility(View.GONE);
                int i = 0;
                for (DocumentSnapshot snapshot: queryDocumentSnapshots){
                    Transaction currentTransaction = snapshot.toObject(Transaction.class);

                    // TODO: fix getType thing and test the transaction item
                    if(currentTransaction.getFrom().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        currentTransaction.setSendByMe(true);
                    }else{
                        currentTransaction.setSendByMe(false);
                    }
                    currentTransaction.loadUserDataFromReference(transactionAdapter);
                    transactionAdapter.add(currentTransaction);
                    progressBar.setVisibility(View.GONE);
                    i++;
                }
                if(i==0 && transactionRecyclerView.getVisibility()==View.VISIBLE){
                    bg_txt_tv.setVisibility(View.VISIBLE);
                    bg_txt_tv.setText("No transaction");
                }
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
        });
    }


}