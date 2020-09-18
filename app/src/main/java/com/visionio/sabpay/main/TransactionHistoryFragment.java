package com.visionio.sabpay.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.visionio.sabpay.InvoiceActivity;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.OrderAdapter;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.models.InvoiceDialog;
import com.visionio.sabpay.models.Order;
import com.visionio.sabpay.models.Transaction;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    long itemLimit = 10;
    Query loadOrderQuery, loadTransactionQuery;
    FirebaseFirestore mRef;
    Chip loadMore_chip;
    View.OnClickListener transactionLoadListener = v -> loadTransactions();
    View.OnClickListener orderLoadListener = v -> loadOrders();
    boolean isAllTransactionsLoaded = false;
    boolean isAllOrdersLoaded = false;
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
        loadMore_chip = view.findViewById(R.id.history_loadMore_chip);
        ((MainActivity) Objects.requireNonNull(getActivity())).setTitle("History");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRef = FirebaseFirestore.getInstance();
        toggleButton.setSingleSelection(true);

        orderAdapter = new OrderAdapter(new ArrayList<>(), (order, position, view1) -> {
            InvoiceDialog invoiceDialog = new InvoiceDialog(getContext(), order);
            //invoiceDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            invoiceDialog.show();
        });
        orderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderRecyclerView.setHasFixedSize(false);
        orderRecyclerView.setAdapter(orderAdapter);

        transactionAdapter = new TransactionAdapter(new ArrayList<>());
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionRecyclerView.setHasFixedSize(false);
        transactionRecyclerView.setAdapter(transactionAdapter);

        transactions.setOnClickListener(view1 -> loadTransactionHistory() );
        orders.setOnClickListener(view1 -> loadOrderHistory());

        toggleButton.check(R.id.btn_order_history);
        orders.callOnClick();
    }

    public void loadOrderHistory() {
        transactionRecyclerView.setVisibility(View.GONE);
        orderRecyclerView.setVisibility(View.VISIBLE);
        loadMore_chip.setOnClickListener(orderLoadListener);
        if(orderAdapter.getItemCount()==0){
            loadOrders();
        }
    }

    void loadOrders(){
        progressBar.setVisibility(View.VISIBLE);
        bg_txt_tv.setVisibility(View.GONE);
        loadMore_chip.setEnabled(false);
        if(loadOrderQuery == null) {
            loadOrderQuery = mRef.collection("order")
                    .whereEqualTo("user.userId", FirebaseAuth.getInstance().getUid())
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(itemLimit);
        }
        if(isAllOrdersLoaded) {
            Utils.toast(getContext(), "No more orders", Toast.LENGTH_SHORT);
            progressBar.setVisibility(View.GONE);
            loadMore_chip.setEnabled(true);
            loadMore_chip.setEnabled(true);
            return;
        }
        loadOrderQuery.get()
                .addOnCompleteListener(task -> {
                    loadMore_chip.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    loadMore_chip.setEnabled(true);
                    if(task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        assert querySnapshot != null;

                        List<Order> orderList = new ArrayList<>();
                        for(DocumentSnapshot documentSnapshot: querySnapshot) {
                            orderList.add(documentSnapshot.toObject(Order.class));
                        }
                        if(orderList.size()!=0) {
                            DocumentSnapshot lastVisible = querySnapshot.getDocuments()
                                    .get(querySnapshot.size() - 1);
                            loadOrderQuery = mRef.collection("order")
                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                    .whereEqualTo("user.userId", FirebaseAuth.getInstance().getUid())
                                    .startAfter(lastVisible)
                                    .limit(itemLimit);
                            orderAdapter.setOrderList(orderList);
                            if(orderList.size()<itemLimit) {
                                isAllOrdersLoaded= true;
                                loadOrderQuery = null;
                            }
                        } else {
                            loadOrderQuery = null;
                            isAllOrdersLoaded = true;
                        }
                    } else {
                        Utils.toast(getActivity(), Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_LONG);
                    }
                });
    }

    public void loadTransactionHistory() {
        orderRecyclerView.setVisibility(View.GONE);
        transactionRecyclerView.setVisibility(View.VISIBLE);
        loadMore_chip.setOnClickListener(transactionLoadListener);
        if(transactionAdapter.getItemCount()==0){
            loadTransactions();
        }

    }

    public void loadTransactions(){
        progressBar.setVisibility(View.VISIBLE);
        bg_txt_tv.setVisibility(View.GONE);
        loadMore_chip.setEnabled(false);
        if(loadTransactionQuery == null) {
            String uid = FirebaseAuth.getInstance().getUid();
            assert uid != null;
            loadTransactionQuery = mRef.collection("user")
                    .document(uid).collection("transaction")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(itemLimit);
        }
        if (isAllTransactionsLoaded) {
            Utils.toast(getContext(), "No more transactions", Toast.LENGTH_SHORT);
            progressBar.setVisibility(View.GONE);
            loadMore_chip.setEnabled(true);
            loadMore_chip.setEnabled(true);
            return;
        }
        loadTransactionQuery.get()
            .addOnCompleteListener(task -> {
                loadMore_chip.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                loadMore_chip.setEnabled(true);
                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    assert querySnapshot != null;
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot: querySnapshot) {
                        Transaction currentTransaction = documentSnapshot.toObject(Transaction.class);
                        assert currentTransaction != null;
                        if (currentTransaction.getFrom().getId().equals(FirebaseAuth.getInstance().getUid())){
                            currentTransaction.setSendByMe(true);
                        } else {
                            currentTransaction.setSendByMe(false);
                        }
                        currentTransaction.loadUserDataFromReference(transactionAdapter);
                        transactions.add(currentTransaction);
                    }
                    if (transactions.size()!=0) {
                        DocumentSnapshot lastVisible = querySnapshot.getDocuments()
                                .get(querySnapshot.size() - 1);
                        String uid = FirebaseAuth.getInstance().getUid();
                        assert uid!=null;
                        loadTransactionQuery = mRef.collection("user")
                                .document(uid).collection("transaction")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .startAfter(lastVisible)
                                .limit(itemLimit);
                        transactionAdapter.setTransactionList(transactions);
                        if (transactions.size()<itemLimit) {
                            isAllTransactionsLoaded = true;
                            loadTransactionQuery = null;
                        }
                    } else {
                        loadTransactionQuery = null;
                        isAllTransactionsLoaded = true;
                    }
                } else {
                    Utils.toast(getActivity(), Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_LONG);
                }
            });
    }
}