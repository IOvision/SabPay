package com.visionio.sabpay.models;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.OrderItemAdapter;
import com.visionio.sabpay.api.API;
import com.visionio.sabpay.helper.InvoiceGenerator;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceDialog extends Dialog implements View.OnClickListener {

    ScrollView root_sv;
    String invoiceId, orderIdString, orderStatus;
    TextView orderTime, orderId, orderAmount, paymentStatus, shipmentAddress;
    ImageButton download_bt, share_bt;
    Order order;
    RecyclerView itemListRecycler;
    OrderItemAdapter adapter;
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
        setContentView(R.layout.order_detail_layout);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        root_sv = findViewById(R.id.order_detail_root_sv);
        progressBar = findViewById(R.id.order_detail_progressBar_pb);
        pay_bt = findViewById(R.id.invoice_activity_pay_bt);
        orderTime = findViewById(R.id.order_time);
        orderId = findViewById(R.id.order_id);
        orderAmount = findViewById(R.id.order_amount);
        paymentStatus = findViewById(R.id.payment_status);
        shipmentAddress = findViewById(R.id.shipment_address);
        itemListRecycler = findViewById(R.id.items_recycler_view);
        download_bt = findViewById(R.id.order_detail_download_ibt);
        share_bt = findViewById(R.id.order_detail_share_ibt);

        if(order.getInvoiceId() == null){

            pay_bt.setVisibility(View.VISIBLE);
        }

        download_bt.setOnClickListener(v -> {
            if(InvoiceGenerator.isInvoiceGenerated(order.getOrderId())){
                Utils.toast(getContext(), "Invoice Already Downloaded", Toast.LENGTH_SHORT);
                return;
            }
            downloadInvoice(false);
        });

        share_bt.setOnClickListener(v -> {
            if(!InvoiceGenerator.isInvoiceGenerated(order.getOrderId())){
                downloadInvoice(true);
                return;
            }
            File outputFile = InvoiceGenerator.getOutputFile(order.getOrderId());
            shareInvoice(outputFile);
        });

        root_sv.fullScroll(ScrollView.FOCUS_UP);

        setTextViews();
    }

    void downloadInvoice(boolean toShare){
        if(invoiceId == null){
            Utils.toast(mContext, "Complete payment to generate invoice", Toast.LENGTH_LONG);
            return;
        }
        pay_bt.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        mRef.collection("inventory").document(order.getFromInventory())
                .get().addOnCompleteListener(task -> {
            pay_bt.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            if(task.isSuccessful()){
                order.shopAddress = task.getResult().getString("address");
                InvoiceGenerator generator = new InvoiceGenerator(order, getContext());
                File file = generator.generate();
                if(file == null){
                    Utils.toast(mContext, "App Error", Toast.LENGTH_LONG);
                }else {
                    Utils.toast(mContext, "Invoice Downloaded To "+file.getPath(), Toast.LENGTH_LONG);
                    if (toShare){
                       shareInvoice(file);
                    }
                }
            }else{
                Utils.toast(getContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT);
            }
        });
    }
    void shareInvoice(File file){
        Intent share = new Intent();
        Uri uri = FileProvider.getUriForFile(
                mContext,
                mContext.getString(R.string.authority),
                file
        );
        share.setAction(Intent.ACTION_SEND);
        share.setType("application/pdf");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().getApplicationContext().startActivity(share);
    }

    void pay(String receiverId) {
        Call<Map<String, Object>> pay = API.getApiService().pay(mAuth.getUid(),
                receiverId, order.getAmount(), API.api_key);
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
        }

        mRef.document(String.format("order/%s", order.getOrderId())).set(order).addOnCompleteListener(task -> {
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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yy");
        String date = simpleDateFormat.format(order.getTimestamp().toDate());
        orderTime.setText(date);
        orderId.setText(orderIdString);
        orderAmount.setText(String.format("\u20B9%s", order.getAmount()));
        shipmentAddress.setText(order.getUser().get("address"));
        if(order.getInvoiceId() != null){
            paymentStatus.setText("Completed using Sabpay");
        }else{
            paymentStatus.setText("Pending");
        }

        loadItems();
    }

    private void loadItems() {
        adapter = new OrderItemAdapter(new ArrayList<>(), mContext);
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
