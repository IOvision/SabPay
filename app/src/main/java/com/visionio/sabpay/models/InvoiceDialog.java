package com.visionio.sabpay.models;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    TextView orderTime, orderId, orderAmount, paymentStatus, baseAmount, discount, promoCode, totalAmount;
    Invoice invoice;
    Order order;
    RecyclerView itemListRecycler;
    InvoiceAdapter adapter;
    List<Item> items;
    ProgressBar progressBar;
    Button pay_bt;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;
    Context mContext;

    BottomSheetDialog invoice_dialog;
    TextView item_count_tv, entity_count_tv, order_from_tv;
    TextView base_amount_tv, delivery_charge_tv;
    TextView total_tv, discount_tv, payable_amount_tv;
    Button payAndOrder_bt, confirmOrder_bt;

    public InvoiceDialog(@NonNull Context context, Order order) {
        super(context);
        this.order = order;
        this.invoiceId = order.getInvoiceId();
        this.orderIdString = order.getOrderId();
        this.orderStatus = order.getStatus();
        this.mContext = context;
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
        baseAmount = findViewById(R.id.base_amount);
        discount = findViewById(R.id.discount);
        promoCode = findViewById(R.id.promo_code);
        totalAmount = findViewById(R.id.total_amount);
        itemListRecycler = findViewById(R.id.items_recycler_view);

        if (order.isPaymentDone()) {
            loadInvoice();
        }else {
            Cart cart = new Cart();
            cart.setItemList(order.getItems());
            cart.itemQtyToCartQty();
            invoice = Invoice.fromItems(cart.getItemList());
            setTextViewsWhenInvoiceIsNotGenerated();

            setUpInvoice();
        }
    }

    private void loadInvoice() {
        FirebaseFirestore.getInstance().collection("user")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .collection("invoice").document(invoiceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    invoice = documentSnapshot.toObject(Invoice.class);
                    Log.d("invoice", "invoice" + invoice.getId());
                    items = invoice.getItems();
                    setTextViews();
                })
                .addOnFailureListener(e -> {

                });
    }

    void setUpInvoice() {
        if(invoice_dialog!=null){
            return;
        }
        if(!order.isPaymentDone() &&
                !order.status.equalsIgnoreCase(Order.STATUS.ORDER_RECEIVED) &&
                !order.status.equalsIgnoreCase(Order.STATUS.ORDER_CANCELLED)){
            pay_bt.setVisibility(View.VISIBLE);
        }

        pay_bt.setOnClickListener(v -> {
            invoice_dialog.show();
            invoice_dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        invoice_dialog = new BottomSheetDialog(mContext);
        invoice_dialog.setContentView(R.layout.invoice_layout);
        invoice_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        invoice_dialog.getWindow().setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        item_count_tv = invoice_dialog.findViewById(R.id.invoice_itemCount_tv);
        entity_count_tv = invoice_dialog.findViewById(R.id.invoice_entityCount_tv);
        order_from_tv = invoice_dialog.findViewById(R.id.invoice_orderFrom_tv);
        base_amount_tv = invoice_dialog.findViewById(R.id.invoice_baseAmt_tv);
        delivery_charge_tv = invoice_dialog.findViewById(R.id.invoice_deliveryCharge_tv);
        total_tv = invoice_dialog.findViewById(R.id.invoice_totalAmt_tv);
        discount_tv = invoice_dialog.findViewById(R.id.invoice_discount_tv);
        payable_amount_tv = invoice_dialog.findViewById(R.id.invoice_payableAmt_tv);
        //promo_tv = invoice_dialog.findViewById(R.id.invoice_promo_tv);

        payAndOrder_bt = invoice_dialog.findViewById(R.id.invoice_pay_and_confirm_bt);
        confirmOrder_bt = invoice_dialog.findViewById(R.id.invoice_confirm_bt);

        BottomSheetBehavior<FrameLayout> behavior = invoice_dialog.getBehavior();

        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    invoice_dialog.dismiss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });


        payAndOrder_bt.setOnClickListener(v -> {
            invoice_dialog.dismiss();
            fetchAndPay();
        });

        confirmOrder_bt.setVisibility(View.GONE);
        payAndOrder_bt.setText("Pay");

        //invoice_dialog.getBehavior().setFitToContents(true);
        //invoice_dialog.getBehavior().setHalfExpandedRatio(0.5f);
        invoice_dialog.getBehavior().setPeekHeight(0);
        invoice_dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        invoice_dialog.setCancelable(false);
        updateInvoice();
    }

    void updateInvoice() {
        String item_count = Invoice.STR_ITEM_COUNT + " " + items.size();
        String entity_count = Invoice.STR_ENTITY_COUNT + " " + Utils.getEntityCount(items);
        String order_from = Invoice.STR_ORDER_FROM + " " + order.getFromInventoryName();

        String base_amt = Utils.equalize(invoice.getBase_amount(), Invoice.STR_BASE_AMOUNT);
        String delivery_charge = Utils.equalize(0, Invoice.STR_DELIVERY_CHARGE);

        String total = Utils.equalize(invoice.getTotal_amount(), Invoice.STR_TOTAL);
        String discount = Utils.equalize(invoice.getDiscount(), Invoice.STR_DISCOUNT);

        String payable_amt = Utils.equalize(invoice.getTotal_amount(), Invoice.STR_PAYABLE_AMOUNT);


        item_count_tv.setText(item_count);
        entity_count_tv.setText(entity_count);
        order_from_tv.setText(order_from);
        base_amount_tv.setText(base_amt);
        delivery_charge_tv.setText(delivery_charge);
        total_tv.setText(total);
        discount_tv.setText(discount);
        payable_amount_tv.setText(payable_amt);

    }

    void fetchAndPay() {
        progressBar.setVisibility(View.VISIBLE);
        mRef.document(String.format("inventory/%s", order.getFromInventory())).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Inventory inventory = Objects.requireNonNull(task.getResult()).toObject(Inventory.class);
                    assert inventory != null;
                    pay(inventory.getOwner().getId());
                } else {
                    progressBar.setVisibility(View.GONE);
                    Utils.toast(mContext, Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_LONG);
                }
            });
    }

    void pay(String receiverId) {
        Call<Map<String, Object>> pay = API.getApiService().pay(mAuth.getUid(),
                receiverId, invoice.getTotal_amount(), API.api_key);
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

        if(order.getStatus().equalsIgnoreCase(Order.STATUS.ORDER_DELIVERED)){
            order.setStatus(Order.STATUS.ORDER_COMPLETE);
            orderUpdate.put("items", null);
        }

        invoice.setId(invoiceId);
        invoice.setPromo(null);
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
        orderAmount.setText("\u20B9" + String.valueOf(invoice.getTotal_amount()));
        paymentStatus.setText("\u20B9" + String.valueOf(invoice.getBase_amount()));
        baseAmount.setText("\u20B9" + String.valueOf(invoice.getBase_amount()));
        discount.setText("\u20B9" + String.valueOf(invoice.getDiscount()));
        promoCode.setText(String.valueOf(invoice.getPromo()));
        totalAmount.setText("\u20B9" + String.valueOf(invoice.getTotal_amount()));
        loadItems();
    }

    private void setTextViewsWhenInvoiceIsNotGenerated() {
        orderTime.setText(String.valueOf(order.getTimestamp().toDate()));
        orderId.setText(order.getOrderId());
        orderAmount.setText("\u20B9" + String.valueOf(order.getAmount()));
        paymentStatus.setText(String.valueOf(order.getStatus()));
        baseAmount.setText("N.A.");
        discount.setText("N.A.");
        promoCode.setText("N.A.");
        totalAmount.setText("\u20B9" + String.valueOf(order.getAmount()));
        loadItems();
    }

    private void loadItems() {
        adapter = new InvoiceAdapter(new ArrayList<>());
        itemListRecycler.setLayoutManager(new LinearLayoutManager(mContext));
        itemListRecycler.setHasFixedSize(false);
        itemListRecycler.setAdapter(adapter);

        for (Item item : items) {
            adapter.add(item);
        }
    }

    @Override
    public void onClick(View view) {

    }
}
