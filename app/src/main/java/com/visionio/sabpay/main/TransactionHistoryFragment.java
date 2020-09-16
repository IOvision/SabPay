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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
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
    long itemLimit = 2;
    Query loadOrderQuery, loadTransactionQuery;
    FirebaseFirestore mRef;
    Chip loadMore_chip;
    boolean isAllItemsLoaded = false;
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
        ((MainActivity)getActivity()).setTitle("History");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRef = FirebaseFirestore.getInstance();
        toggleButton.setSingleSelection(true);

        loadMore_chip.setOnClickListener(view1 -> {
            if (toggleButton.getCheckedButtonId() == R.id.btn_transaction_history)
                loadTransactions();
            else
                loadOrders();
        });

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
        isAllItemsLoaded = false;
        transactionRecyclerView.setVisibility(View.GONE);
        orderRecyclerView.setVisibility(View.VISIBLE);
        orderAdapter = new OrderAdapter(new ArrayList<>(), (order, position, view) -> {
                Intent i = new Intent(getActivity(), InvoiceActivity.class);
                String orderJson = new Gson().toJson(order);
                i.putExtra("order", orderJson);
                i.putExtra("invoiceId", order.getInvoiceId());
                i.putExtra("orderId", order.getOrderId());
                i.putExtra("orderStatus", order.getStatus());
                startActivity(i);
        });
        orderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderRecyclerView.setHasFixedSize(false);
        orderRecyclerView.setAdapter(orderAdapter);
        loadOrders();
    }


    void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        bg_txt_tv.setVisibility(View.GONE);

        if(loadOrderQuery == null) {
            loadOrderQuery = mRef.collection("order")
                    .whereEqualTo("user.userId", FirebaseAuth.getInstance().getUid())
                    .orderBy("timestamp")
                    .limit(itemLimit);
        }
        if(isAllItemsLoaded) {
            Utils.toast(getContext(), "No more items", Toast.LENGTH_SHORT);
            progressBar.setVisibility(View.GONE);
            loadMore_chip.setEnabled(true);
            return;
        }
        loadOrderQuery.get()
                .addOnCompleteListener(task -> {
                    loadMore_chip.setEnabled(true);
                    if(task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        Log.d("testing", "size: " + task.getResult().size());
                        assert querySnapshot != null;

                        List<Order> orderList = new ArrayList<>();
                        for(DocumentSnapshot documentSnapshot: querySnapshot) {
                            orderAdapter.add(documentSnapshot.toObject(Order.class));
                            orderList.add(documentSnapshot.toObject(Order.class));
                        }
                        if(orderList.size()!=0) {
                            DocumentSnapshot lastVisible = querySnapshot.getDocuments()
                                    .get(querySnapshot.size() - 1);
                            loadOrderQuery = mRef.collection("order")
                                    .orderBy("timestamp")
                                    .whereEqualTo("user.userId", FirebaseAuth.getInstance().getUid())
                                    .startAfter(lastVisible)
                                    .limit(itemLimit);
                            orderAdapter.setOrderList(orderList);
                            if(orderList.size()<itemLimit) {
                                isAllItemsLoaded = true;
                                loadOrderQuery = null;
                            } else {
                                loadOrderQuery = null;
                                isAllItemsLoaded = true;
                            }
                        } else {
                            loadOrderQuery = null;
                            isAllItemsLoaded = true;
                        }
                    } else {
                        Log.i("toast", task.getException().getLocalizedMessage());
                        Utils.toast(getActivity(), task.getException().getLocalizedMessage(), Toast.LENGTH_LONG);
                    }
                });
    }

    public void loadTransactionHistory() {
        isAllItemsLoaded = false;
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

        if(loadTransactionQuery == null) {
            loadTransactionQuery = mRef.collection("user")
                    .document(FirebaseAuth.getInstance().getUid()).collection("transaction")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(itemLimit);
        }
        if (isAllItemsLoaded) {
            Utils.toast(getContext(), "No more items", Toast.LENGTH_SHORT);
            progressBar.setVisibility(View.GONE);
            loadMore_chip.setEnabled(true);
            return;
        }
        loadTransactionQuery.get()
            .addOnCompleteListener(task -> {
                loadMore_chip.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    assert querySnapshot != null;
                    Log.d("testing", "size: " + task.getResult().size());
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot: querySnapshot) {
                        Transaction currentTransaction = documentSnapshot.toObject(Transaction.class);
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
                        loadTransactionQuery = mRef.collection("user")
                                .document(FirebaseAuth.getInstance().getUid()).collection("transaction")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .startAfter(lastVisible)
                                .limit(itemLimit);
                        transactionAdapter.setTransactionList(transactions);
                        if (transactions.size()<itemLimit) {
                            isAllItemsLoaded = true;
                            loadTransactionQuery = null;
                        }
                    } else {
                        loadTransactionQuery = null;
                        isAllItemsLoaded = true;
                    }
                } else {
                    Log.i("toast", task.getException().getLocalizedMessage());
                    Utils.toast(getActivity(), task.getException().getLocalizedMessage(), Toast.LENGTH_LONG);
                }
            });
    }
}