package com.visionio.sabpay.models;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.google.gson.Gson;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.InvoiceAdapter;
import com.visionio.sabpay.api.API;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceDialog extends Dialog implements View.OnClickListener {

    String invoiceId, orderIdString, orderStatus;
    TextView orderTime, orderId, orderAmount, paymentStatus, shipmentAddress;
    Invoice invoice;
    Order order;
    RecyclerView itemListRecycler;
    InvoiceAdapter adapter;
    List<CompressedItem> items;
    ProgressBar progressBar;
    Button pay_bt;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;
    Context mContext;

    String inventoryId;

    public InvoiceDialog(@NonNull Context context, Order order, String inventoryId) {
        super(context);
        if(order.items != null) {
            this.items = order.items;
        }
        this.order = order;
        this.invoiceId = order.getInvoiceId();
        this.orderIdString = order.getOrderId();
        this.orderStatus = order.getStatus();
        this.mContext = context;
        this.inventoryId = inventoryId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_invoice);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.invoice_activity_progressBar_pb);
        pay_bt = findViewById(R.id.invoice_activity_pay_bt);
        orderTime = findViewById(R.id.order_time);
        orderId = findViewById(R.id.order_id);
        orderAmount = findViewById(R.id.order_amount);
        paymentStatus = findViewById(R.id.payment_status);
        itemListRecycler = findViewById(R.id.items_recycler_view);

        setTextViews();
    }

    void pay(String receiverId) {
        Call<Map<String, Object>> pay = API.getApiService().pay(mAuth.getUid(),
                receiverId, invoice.getAmount(), API.api_key);
        pay.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Map<String, Object> result;
                if (!response.isSuccessful()) {
                    String body = null;
                    try {
                        assert response.errorBody() != null;
                        body = response.errorBody().string();
                    } catch (IOException e) {
                        body = "{}";
                    }
                    result = new Gson().fromJson(body, HashMap.class);
                    Utils.toast(mContext,
                            Objects.requireNonNull(result.get("error")).toString(), Toast.LENGTH_LONG);
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                result = response.body();
                assert result != null;
                if (result.containsKey("error")) {
                    Utils.toast(mContext,
                            Objects.requireNonNull(result.get("error")).toString(), Toast.LENGTH_LONG);
                    progressBar.setVisibility(View.GONE);
                } else {
                    String s = String.format("Status: %s\nFrom: %s\nAmount: %s\nTransaction Id: %s",
                            result.get("status"), result.get("from"), result.get("amount"), result.get("transactionId"));
                    Utils.toast(mContext, s, Toast.LENGTH_LONG);
                    String transactionId = result.get("transactionId").toString();
                    placeOrder(transactionId);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.i("test", "onFailure: " + t.getLocalizedMessage());
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    void placeOrder(String transactionId) {
        String invoiceId = mRef.collection(String.format("user/%s/invoice", mAuth.getUid())).document().getId();
        order.setInvoiceId(invoiceId);

        Map<String, Object> orderUpdate = new HashMap<>();
        orderUpdate.put("transactionId", transactionId);
        orderUpdate.put("invoiceId", invoiceId);
        orderUpdate.put("status", order.getStatus());
        orderUpdate.put("active", order.getActive());
        if(order.getStatus().equalsIgnoreCase(Order.STATUS.ORDER_DELIVERED)) {
            orderUpdate.put("items", null);

        }

        if(order.getStatus().equalsIgnoreCase(Order.STATUS.ORDER_DELIVERED)){
            order.setStatus(Order.STATUS.ORDER_COMPLETE);
            orderUpdate.put("items", null);
        }

        invoice.setId(invoiceId);
        invoice.setTimestamp(new Timestamp(new Date()));
        invoice.setTransaction(transactionId);


        mRef.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference orderRef = mRef.document("order/" + order.getOrderId());

            String path = String.format("user/%s/invoice/%s", order.getUser().get("userId"), invoiceId);
            DocumentReference invoiceRef = mRef.document(path);

            transaction.update(orderRef, orderUpdate);
            transaction.set(invoiceRef, invoice);

            return null;
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Utils.toast(getContext(), "Paid Successfully", Toast.LENGTH_LONG);
                pay_bt.setVisibility(View.GONE);
                String s = String.format("OrderId: %s\nInvoiceId: %s", order.getOrderId(), order.getInvoiceId());
                Log.i("test", "onComplete: " + s);
            } else {
                Log.i("test", "onComplete: " + task.getException().getLocalizedMessage());
            }
            progressBar.setVisibility(View.GONE);
        });
    }

    private void setTextViews() {
        orderTime.setText(String.valueOf(invoice.getTimestamp().toDate()));
        orderId.setText(orderIdString);
        orderAmount.setText(String.format("\u20B9%s", invoice.getAmount()));
        if(order.getInvoiceId() != null){
            paymentStatus.setText("Completed using Sabpay");
        }else{
            paymentStatus.setText("Pending");
        }

        loadItems();
    }

    private void loadItems() {
        adapter = new InvoiceAdapter(new ArrayList<>(), mContext);
        itemListRecycler.setLayoutManager(new LinearLayoutManager(mContext));
        itemListRecycler.setHasFixedSize(false);
        itemListRecycler.setAdapter(adapter);

        for (CompressedItem item : items) {
            adapter.add(item);
        }
    }

    @Override
    public void onClick(View view) {

    }
}
