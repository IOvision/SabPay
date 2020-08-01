package com.visionio.sabpay.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.smarteist.autoimageslider.SliderView;
import com.visionio.sabpay.R;
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

    void addToCart(Item i) {
        Item item = i.copy();


        for (Item it : cart) {
            if (it.getId().equals(item.getId())) {
                if (item.getQty() - it.getQty() >= 1) {
                    it.setQty(it.getQty() + 1);
                }
                Utils.toast(this, String.format("1 more unit of %s Added", it.getTitle()), Toast.LENGTH_SHORT);
                return;
            }
        }


        item.setQty(1);
        cart.add(i);
        cart_fab.setText(String.format("%s", cart.size()));
        Utils.toast(this, String.format("1 unit of %s Added", i.getTitle()), Toast.LENGTH_SHORT);
    }


}