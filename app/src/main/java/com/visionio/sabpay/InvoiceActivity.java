package com.visionio.sabpay;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.adapter.InvoiceAdapter;
import com.visionio.sabpay.models.Invoice;
import com.visionio.sabpay.models.Item;
import com.visionio.sabpay.models.Order;

import java.util.ArrayList;

public class InvoiceActivity extends AppCompatActivity {

    String invoiceId, orderIdString, orderStatus;
    TextView orderTime, orderId, orderAmount, paymentStatus, baseAmount, discount, promoCode, totalAmount;
    Invoice invoice;
    RecyclerView itemListRecycler;
    InvoiceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            invoiceId = extras.getString("invoiceId");
            orderIdString = extras.getString("orderId");
            orderStatus = extras.getString("orderStatus");
        }

        orderTime = findViewById(R.id.order_time);
        orderId = findViewById(R.id.order_id);
        orderAmount = findViewById(R.id.order_amount);
        paymentStatus = findViewById(R.id.payment_status);
        baseAmount = findViewById(R.id.base_amount);
        discount = findViewById(R.id.discount);
        promoCode = findViewById(R.id.promo_code);
        totalAmount = findViewById(R.id.total_amount);
        itemListRecycler = findViewById(R.id.items_recycler_view);

        if(orderStatus.equals(Order.STATUS_ORDER_COMPLETED)){
            loadInvoice();
        }

        //loadInvoice();
    }

    private void loadInvoice() {
        FirebaseFirestore.getInstance().collection("user")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("invoice").document(invoiceId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        invoice = documentSnapshot.toObject(Invoice.class);
                        Log.d("invoice","invoice" + invoice.getId());
                        setTextViews();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }
// todo: update method to work for both if invoice is generated and if it isnt
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

    private void loadItems() {
        Log.d("Testing", "invoice: " + String.valueOf(invoice.getItems()));
        adapter = new InvoiceAdapter(new ArrayList<>());
        itemListRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        itemListRecycler.setHasFixedSize(false);
        itemListRecycler.setAdapter(adapter);

        for(Item item : invoice.getItems()) {
            adapter.add(item);
        }
        Log.d("Testing", "invoice: " + adapter.getItemCount());
    }

}