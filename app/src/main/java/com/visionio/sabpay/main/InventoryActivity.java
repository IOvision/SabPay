package com.visionio.sabpay.main;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.smarteist.autoimageslider.SliderView;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.CartItemAdapter;
import com.visionio.sabpay.adapter.InventoryItemAdapter;
import com.visionio.sabpay.adapter.SimpleImageAdapter;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Inventory;
import com.visionio.sabpay.models.Item;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    Inventory mInventory;

    SliderView inv_images_sv;

    InventoryItemAdapter adapter;
    RecyclerView recyclerView;

    FirebaseFirestore mRef;

    ExtendedFloatingActionButton cart_fab;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        Toolbar toolbar = findViewById(R.id.inv_activity_toolbar);
        setSupportActionBar(toolbar);
        mRef = FirebaseFirestore.getInstance();

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
                cart_fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideCart();
                        cart_fab.setOnClickListener(listener);
                    }
                });
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
        if(invoice_dialog!=null){
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
        promo_tv = invoice_dialog.findViewById(R.id.invoice_promo_tv);

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

        //invoice_dialog.getBehavior().setFitToContents(true);
        //invoice_dialog.getBehavior().setHalfExpandedRatio(0.5f);
        invoice_dialog.getBehavior().setPeekHeight(0);
        invoice_dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        invoice_dialog.setCancelable(false);

        invoice_dialog.show();

    }
    void addToCart(Item i) {
        //adapter.addToCart(i);
        Item item = i.copy();
        for (Item it : cart) {
            if (it.equals(i)) {
                it.addToCart();
                Utils.toast(this, String.format("1 more unit of %s Added", it.getTitle()), Toast.LENGTH_SHORT);
                return;
            }
        }
        item.addToCart();
        cart.add(item);
        cart_fab.setText(String.format("%s", cart.size()));
        Utils.toast(this, String.format("1 unit of %s Added", i.getTitle()), Toast.LENGTH_SHORT);
    }




}