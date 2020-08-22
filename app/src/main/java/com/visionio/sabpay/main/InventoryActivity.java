package com.visionio.sabpay.main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.gson.Gson;
import com.smarteist.autoimageslider.SliderView;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.CartItemAdapter;
import com.visionio.sabpay.adapter.InventoryItemAdapter;
import com.visionio.sabpay.adapter.SimpleImageAdapter;
import com.visionio.sabpay.api.API;
import com.visionio.sabpay.api.SabPayNotify;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Inventory;
import com.visionio.sabpay.models.Invoice;
import com.visionio.sabpay.models.Item;
import com.visionio.sabpay.models.Order;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.paperdb.Paper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InventoryActivity extends AppCompatActivity {

    Inventory mInventory;

    SliderView inv_images_sv;

    InventoryItemAdapter adapter;
    RecyclerView recyclerView;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    ExtendedFloatingActionButton cart_fab;

    //TODO: Create separate static class for cart
    List<Item> cart;

    // dialog views
    Dialog cart_dialog;
    Button dialog_confirm_bt;
    RecyclerView dialog_items_rv;
    CartItemAdapter dialog_cart_adapter;

    // bottom sheet for invoice and its related layout
    BottomSheetDialog invoice_dialog;
    TextView item_count_tv, entity_count_tv, order_from_tv;
    TextView base_amount_tv, delivery_charge_tv;
    TextView total_tv, discount_tv, payable_amount_tv, promo_tv ;
    Button payAndOrder_bt, confirmOrder_bt;
    Invoice mInvoice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        Toolbar toolbar = findViewById(R.id.inv_activity_toolbar);
        setSupportActionBar(toolbar);
        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setup();
    }

    private void setup() {
        Intent i = getIntent();
        //mInventory = (Inventory)i.getSerializableExtra("inventory");
        String json = i.getStringExtra("inventory");
        mInventory = Inventory.formJson(json);
        getSupportActionBar().setTitle(mInventory.getName());
        cart = new ArrayList<>();

        cart_fab = findViewById(R.id.inv_activity_cart_exFab);
        inv_images_sv = findViewById(R.id.inv_activity_items_image_sv);
        recyclerView = findViewById(R.id.inv_activity_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        adapter = new InventoryItemAdapter(this, new ArrayList<>());
        adapter.setClickListener(new OnItemClickListener<Item>() {
            @Override
            public void onItemClicked(Item object, int position, View view) {
                addToCart(object);
                //Utils.toast(InventoryActivity.this, object.getTitle(), Toast.LENGTH_LONG);
            }
        });
        recyclerView.setAdapter(adapter);

        inv_images_sv.setSliderAdapter(new SimpleImageAdapter(this) {{
            setImageUrls(mInventory.getImages());
        }});


        cart_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCart();
                View.OnClickListener listener = this;
            }
        });

        loadItems(mInventory.getItems());
    }

    void loadItems(List<String> items) {
        mRef.collection("item").whereIn("id", items).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            assert querySnapshot != null;
                            List<Item> itemList = new ArrayList<>();
                            for (DocumentSnapshot documentSnapshot : querySnapshot) {
                                itemList.add(documentSnapshot.toObject(Item.class));
                            }
                            adapter.setItemList(itemList);
                        } else {
                            Utils.toast(InventoryActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_LONG);
                        }
                    }
                });
    }

    void hideCart(){
        if(cart_dialog==null){
            return;
        }
        cart_dialog.hide();
    }
    void showCart(){
        if(cart_dialog!=null){
            dialog_cart_adapter.setItemList(new ArrayList<>(cart));
            cart_dialog.show();
            return;
        }
        setUpCart();
    }
    void setUpCart(){
        cart_dialog = new Dialog(this);
        cart_dialog.setContentView(R.layout.cart_layout);
        cart_dialog.getWindow().setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        dialog_confirm_bt = cart_dialog.findViewById(R.id.cart_layout_confirm_bt);
        dialog_items_rv = cart_dialog.findViewById(R.id.cart_layout_itemList_rv);
        dialog_items_rv.setLayoutManager(new LinearLayoutManager(this));
        dialog_items_rv.setHasFixedSize(false);

        dialog_cart_adapter = new CartItemAdapter(new ArrayList<>(cart), this);

        dialog_items_rv.setAdapter(dialog_cart_adapter);

        dialog_confirm_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInvoice();
            }
        });

        cart_dialog.show();
    }

    void showInvoice(){
        mInvoice = Invoice.fromItems(cart);
        if(invoice_dialog!=null){
            updateInvoice();
            invoice_dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            invoice_dialog.show();
            return;
        }
        setUpInvoice();
    }

    void setUpInvoice(){

        invoice_dialog = new BottomSheetDialog(this);
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
                if(newState == BottomSheetBehavior.STATE_COLLAPSED){
                    invoice_dialog.dismiss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });


        payAndOrder_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(item_count_tv.getText().equals(Invoice.STR_ITEM_COUNT+" 0")) {
                    Toast.makeText(InventoryActivity.this, "There are no items in the cart. ", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(InventoryActivity.this)
                            .setTitle("Proceed Further")
                            .setMessage("Are you sure? Money will be automatically deducted from your SabPay wallet ")

                            .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    pay();
                                }
                            })

                            .setNegativeButton("Cancel", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        });
        confirmOrder_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(item_count_tv.getText().equals(Invoice.STR_ITEM_COUNT+" 0")) {
                    Toast.makeText(InventoryActivity.this, "There are no items in the cart. ", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(InventoryActivity.this)
                            .setTitle("Proceed Further")
                            .setMessage("Are you sure?")

                            .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    placeOrder(null);
                                }
                            })

                            .setNegativeButton("Cancel", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        });

        //invoice_dialog.getBehavior().setFitToContents(true);
        //invoice_dialog.getBehavior().setHalfExpandedRatio(0.5f);
        invoice_dialog.getBehavior().setPeekHeight(0);
        invoice_dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        invoice_dialog.setCancelable(false);

        updateInvoice();
        invoice_dialog.show();

    }
    void updateInvoice(){
        String item_count = Invoice.STR_ITEM_COUNT+" "+cart.size();
        String entity_count = Invoice.STR_ENTITY_COUNT+" "+Utils.getEntityCount(cart);
        String order_from = Invoice.STR_ORDER_FROM+" "+mInventory.getName();

        String base_amt = Utils.equalize(mInvoice.getBase_amount(), Invoice.STR_BASE_AMOUNT);
        String delivery_charge = Utils.equalize(0, Invoice.STR_DELIVERY_CHARGE);

        String total = Utils.equalize(mInvoice.getTotal_amount(), Invoice.STR_TOTAL);
        String discount = Utils.equalize(mInvoice.getDiscount(), Invoice.STR_DISCOUNT);

        String payable_amt = Utils.equalize(mInvoice.getTotal_amount(), Invoice.STR_PAYABLE_AMOUNT);


        item_count_tv.setText(item_count);
        entity_count_tv.setText(entity_count);
        order_from_tv.setText(order_from);
        base_amount_tv.setText(base_amt);
        delivery_charge_tv.setText(delivery_charge);
        total_tv.setText(total);
        discount_tv.setText(discount);
        payable_amount_tv.setText(payable_amt);

    }

    void pay(){
        Call<Map<String, Object>> pay = API.getApiService().pay(mAuth.getUid(),
               mInventory.getOwner().getId(), mInvoice.getTotal_amount(), API.api_key);
        pay.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Map<String, Object> result;
                if(!response.isSuccessful()){
                    String body = null;
                    try {
                        body = response.errorBody().string();
                    } catch (IOException e) {
                        body = "{}";
                    }
                    result = new Gson().fromJson(body, HashMap.class);
                    Utils.toast(InventoryActivity.this,
                            Objects.requireNonNull(result.get("error")).toString(), Toast.LENGTH_LONG);
                    return;
                }
                result = response.body();
                if(result.containsKey("error")){
                    Utils.toast(InventoryActivity.this,
                            Objects.requireNonNull(result.get("error")).toString(), Toast.LENGTH_LONG);
                }else{
                    String s = String.format("Status: %s\nFrom: %s\nAmount: %s\nTransaction Id: %s",
                            result.get("status"), result.get("from"), result.get("amount"), result.get("transactionId"));
                    Utils.toast(InventoryActivity.this, s, Toast.LENGTH_LONG);
                    String transactionId =  result.get("transactionId").toString();
                    placeOrder(transactionId);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.i("test", "onFailure: "+t.getLocalizedMessage());
                int a = 0;
            }
        });
    }

    void placeOrder(String transactionId){
        Order order = new Order();
        order.setOrderId(mRef.collection("order").document().getId());
        order.setAmount(mInvoice.getTotal_amount());
        order.setFromInventory(mInventory.getId());
        User user = Paper.book("user").read("user");
        order.setUser(new HashMap<String, String>(){{
            put("userId", user.getUid());
            put("firstName", user.getFirstName());
            put("lastName", user.getLastName());
            put("phone", user.getPhone());
        }});
        order.setTimestamp(new Timestamp(new Date()));
        order.setStatus(Order.STATUS.ORDER_PLACED);
        if(transactionId==null){
            List<Item> it = new ArrayList<>(mInvoice.getItems());
            for(Item i:it){
                i.setQty(i.getCart_qty());
                if(i.getQty()==0){
                    it.remove(i);
                }
            }
            order.setItems(it);
            order.setTransactionId(null);
            order.setInvoiceId(null);
        }else{
            String invoiceId = mRef.collection(String.format("user/%s/invoice", mAuth.getUid())).document().getId();
            order.setItems(null);
            order.setTransactionId(transactionId);
            order.setInvoiceId(invoiceId);
            mInvoice.setId(invoiceId);
            mInvoice.setPromo(null);
            mInvoice.setTimestamp(new Timestamp(new Date()));
            mInvoice.setTransaction(transactionId);
        }

        mRef.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentReference orderRef = mRef.document("order/"+order.getOrderId());
                if(order.getInvoiceId()!=null){
                    String path = String.format("user/%s/invoice/%s", order.getUser().get("userId"), order.getInvoiceId());
                    DocumentReference invoiceRef = mRef.document(path);

                    transaction.set(orderRef, order);
                    transaction.set(invoiceRef, mInvoice);
                }else{
                    transaction.set(orderRef, order);
                }
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Utils.toast(InventoryActivity.this, "Order Placed Successfully", Toast.LENGTH_LONG);
                    String s = String.format("OrderId: %s\nInvoiceId: %s", order.getOrderId(), order.getInvoiceId());
                    Log.i("test", "onComplete: "+s);
                    String msg = String.format("Order Id: %s\nAmount: Rs. %s\nFrom: %s",
                            order.getOrderId(),
                            order.getAmount(),
                            order.getUser().get("firstName"));
                    new SabPayNotify.Builder()
                            .setTitle("New Order")
                            .setMessage(msg)
                            .send(getApplicationContext(), mInventory.getOwner().getId(), true);



                }else{
                    Log.i("test", "onComplete: "+task.getException().getLocalizedMessage());
                }
                cart_dialog.dismiss();
                invoice_dialog.dismiss();
                finish();
            }
        });


        //order.setAmount();
    }
    //TODO: Increase quantity when clicked multiple times
    void addToCart(Item i) {
        //adapter.addToCart(i);
        Item item = i.copy();
        for (Item it : cart) {
            if (it.equals(i)) {
                it.addToCart();
                Utils.toast(this, String.format("Item already in cart", it.getTitle()), Toast.LENGTH_SHORT);
                return;
            }
        }
        item.addToCart();
        cart.add(item);
        cart_fab.setText(String.format("%s", cart.size()));
        Utils.toast(this, String.format("1 unit of %s Added", i.getTitle()), Toast.LENGTH_SHORT);
    }

}