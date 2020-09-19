package com.visionio.sabpay.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.gson.Gson;
import com.smarteist.autoimageslider.SliderView;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.CartItemAdapter;
import com.visionio.sabpay.adapter.InventoryItemAdapter;
import com.visionio.sabpay.adapter.SearchListAdapter;
import com.visionio.sabpay.adapter.SimpleImageAdapter;
import com.visionio.sabpay.api.API;
import com.visionio.sabpay.api.SabPayNotify;
import com.visionio.sabpay.interfaces.CartListener;
import com.visionio.sabpay.models.Cart;
import com.visionio.sabpay.models.Inventory;
import com.visionio.sabpay.models.Invoice;
import com.visionio.sabpay.models.Item;
import com.visionio.sabpay.models.Order;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;

import org.jetbrains.annotations.NotNull;

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

    CollapsingToolbarLayout collapsingToolbarLayout;
    AppBarLayout appBarLayout;

    MaterialToolbar toolbar;
    EditText search;
    ImageView searchClose;
    RecyclerView searchRecycler;
    SearchListAdapter searchListAdapter;

    InventoryItemAdapter adapter;
    RecyclerView recyclerView;
    NestedScrollView nestedScrollView;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    ExtendedFloatingActionButton cart_fab;


    // pagination query
    Chip loadMore_chip;
    Query loadItemQuery;
    long itemLimit = 10;
    boolean isAllItemsLoaded = false;

    // dialog views
    Dialog cart_dialog;
    ProgressBar cart_dialog_pb;
    TextInputLayout delivery_address_til;
    String delivery_address;
    Button dialog_confirm_bt;
    RecyclerView dialog_items_rv;
    CartItemAdapter dialog_cart_adapter;

    // bottom sheet for invoice and its related layout
    BottomSheetDialog invoice_dialog;
    TextView item_count_tv, entity_count_tv, order_from_tv;
    TextView base_amount_tv, delivery_charge_tv;
    TextView total_tv, discount_tv, payable_amount_tv;
    Button payAndOrder_bt, confirmOrder_bt;
    Invoice mInvoice;

    List<String> searchList;

    Cart newCart;
    CartListener cartListener = new CartListener() {
        @Override
        public void onIncreaseQty(Item item) {
            newCart.addItem(item);
            cart_fab.setText(String.format("%s", newCart.getItemCount()));
        }

        @Override
        public void onDecreaseQty(Item item) {
            newCart.decreaseItem(item);
            cart_fab.setText(String.format("%s", newCart.getItemCount()));
            if (dialog_cart_adapter != null) {
                dialog_cart_adapter.notifyDataSetChanged();
                if(dialog_cart_adapter.getItemCount()==0){
                    cart_dialog.dismiss();
                }
            }
        }
    };
    boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        toolbar = findViewById(R.id.inv_activity_toolbar);
        setSupportActionBar(toolbar);
        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        searchList = new ArrayList<>();
        setup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.inventory_appbar, menu);
        return true;
    }

    private void setup() {
        Intent i = getIntent();
        //mInventory = (Inventory)i.getSerializableExtra("inventory");
        String json = i.getStringExtra("inventory");
        mInventory = Inventory.formJson(json);
        Objects.requireNonNull(getSupportActionBar()).setTitle(mInventory.getName());
        newCart = new Cart();

        searchRecycler = findViewById(R.id.inventory_search_recycler);
        searchClose = findViewById(R.id.inventory_image_close);
        search = findViewById(R.id.inventory_search_et);
        toolbar = findViewById(R.id.inv_activity_toolbar);
        nestedScrollView = findViewById(R.id.inv_activity_rv_nestedScrollView);
        cart_fab = findViewById(R.id.inv_activity_cart_exFab);
        inv_images_sv = findViewById(R.id.inv_activity_items_image_sv);
        recyclerView = findViewById(R.id.inv_activity_rv);
        loadMore_chip = findViewById(R.id.inv_activity_loadMore_chip);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        appBarLayout = findViewById(R.id.inv_activity_app_bar_layout);
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            verticalOffset = Math.abs(verticalOffset)/5;
            float flexibleSpace = appBarLayout.getTotalScrollRange() - verticalOffset;
            Log.i("test", "onCreate: "+verticalOffset);
            cart_fab.animate().translationY(verticalOffset).start();
        });

        adapter = new InventoryItemAdapter(this, new ArrayList<>());
        searchListAdapter = new SearchListAdapter(new ArrayList<>(), (object, position, view) -> {
            for (Item it : newCart.getItemList()) {
                if (it.getId().equals(object)) {
                    newCart.addItem(it);
                    Utils.toast(InventoryActivity.this, String.format("%s already in cart", it.getTitle()), Toast.LENGTH_SHORT);
                    return;
                }
            }
            FirebaseFirestore.getInstance().collection("item").document(object)
                    .get().addOnSuccessListener(snapshot -> {
                Item item = snapshot.toObject(Item.class);
                assert item != null;
                newCart.addItem(item);
            });
        });


        searchRecycler.setLayoutManager(new LinearLayoutManager(this));
        searchRecycler.setHasFixedSize(false);
        searchRecycler.setAdapter(searchListAdapter);

        adapter.setClickListener(cartListener);

        recyclerView.setAdapter(adapter);

        inv_images_sv.setSliderAdapter(new SimpleImageAdapter(this) {{
            setImageUrls(mInventory.getImages());
        }});

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.inventory_appbar_info){
                showInfoDialog();
                return true;
            }
            else if (item.getItemId() == R.id.inventory_appbar_search) {
                searchClose.setVisibility(View.VISIBLE);
                search.setVisibility(View.VISIBLE);
                searchRecycler.setVisibility(View.VISIBLE);
                search.requestFocus();
                return true;
            }
            return false;
        });

        searchClose.setOnClickListener(v -> {
            searchClose.setVisibility(View.GONE);
            search.setVisibility(View.GONE);
            searchRecycler.setVisibility(View.GONE);
            searchListAdapter.clear();
            search.setText("");
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}  @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) {
                searchListAdapter.clear();
                for(String s : mInventory.getItems()) {
                    if (s.toLowerCase().startsWith(editable.toString().toLowerCase()) && !editable.toString().isEmpty()) {
                        searchListAdapter.add(s);
                    } else if (editable.toString().isEmpty()) {
                        searchListAdapter.clear();
                    }
                }
            }
        });

        cart_fab.setOnClickListener(v -> {
            if(newCart.getItemCount() == 0){
                Utils.toast(InventoryActivity.this, "No items in cart", Toast.LENGTH_SHORT);
                return;
            }
            showCart();
            //View.OnClickListener listener = this;
        });

        nestedScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    View view = nestedScrollView.getChildAt(nestedScrollView.getChildCount() - 1);
                    int diff = (view.getBottom() - (nestedScrollView.getHeight() + nestedScrollView.getScrollY()));
                    if (diff == 0) {
                        isLoading = true;
                        loadItems(mInventory.getId());
                        Toast.makeText(InventoryActivity.this, "End of ScrollView", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        loadMore_chip.setOnClickListener(v -> {
            loadMore_chip.setEnabled(false);
            loadItems(mInventory.getId());
        });

        loadItems(mInventory.getId());
    }

    private void showInfoDialog() {
        Dialog dialog = new Dialog(InventoryActivity.this);
        dialog.setContentView(R.layout.shop_info_layout);
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        TextView shopName, ownerName, email, phone;
        ExtendedFloatingActionButton call;

        shopName = dialog.findViewById(R.id.shop_info_shop_name);
        ownerName = dialog.findViewById(R.id.shop_info_owner_name);
        email = dialog.findViewById(R.id.shop_info_email);
        phone = dialog.findViewById(R.id.shop_info_number);
        call = dialog.findViewById(R.id.shop_info_call);

        shopName.setText(mInventory.getName());
        mRef.collection("user").document(mInventory.getOwner().getId()).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        User inventoryUser = Objects.requireNonNull(task.getResult()).toObject(User.class);
                        assert inventoryUser != null;
                        ownerName.setText(inventoryUser.getName());
                        email.setText(inventoryUser.getEmail());
                        phone.setText(inventoryUser.getPhone());
                        call.setOnClickListener(v -> {
                            String SPhone = phone.getText().toString();
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", SPhone, null));
                            startActivity(intent);
                        });
                        dialog.show();
                    } else {
                        Toast.makeText(InventoryActivity.this, "Some error occurred", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void loadItems(String inv_id) {
        Log.d("testing", "loadItems: Loading Items");
        if(loadItemQuery==null){
            loadItemQuery = mRef.collection("item")
                    .orderBy("title")
                    .whereArrayContains("inventories", inv_id)
                    .limit(itemLimit);
        }
        if(isAllItemsLoaded){
            Utils.toast(this, "No more items", Toast.LENGTH_SHORT);
            loadMore_chip.setEnabled(true);
            isLoading = false;
            return;
        }
        loadItemQuery.get()
                .addOnCompleteListener(task -> {
                    loadMore_chip.setEnabled(true);
                    isLoading = false;
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();

                        assert querySnapshot != null;

                        List<Item> itemList = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : querySnapshot) {
                            itemList.add(documentSnapshot.toObject(Item.class));
                        }

                        if(itemList.size()!=0){
                            DocumentSnapshot lastVisible = querySnapshot.getDocuments()
                                    .get(querySnapshot.size() -1);
                            loadItemQuery = mRef.collection("item")
                                    .orderBy("title")
                                    .whereArrayContains("inventories", inv_id)
                                    .startAfter(lastVisible)
                                    .limit(itemLimit);
                            adapter.setItemList(itemList);
                            if(itemList.size()<itemLimit){
                                isAllItemsLoaded = true;
                                loadItemQuery = null;
                            }
                        }else{
                            loadItemQuery = null;
                            isAllItemsLoaded = true;
                        }
                    } else {
                        Utils.toast(InventoryActivity.this,
                                Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_LONG);
                    }
                });
    }

    void showCart(){
        if(cart_dialog!=null){
            dialog_cart_adapter.setItemList(newCart.getItemList());
            cart_dialog.show();
            return;
        }
        setUpCart();
    }
    void setUpCart(){
        cart_dialog = new Dialog(this);
        cart_dialog.setContentView(R.layout.cart_layout);
        Window window = cart_dialog.getWindow();
        assert window!=null;
        window.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        cart_dialog_pb = cart_dialog.findViewById(R.id.order_placing_pb);
        dialog_confirm_bt = cart_dialog.findViewById(R.id.cart_layout_confirm_bt);
        delivery_address_til = cart_dialog.findViewById(R.id.cart_layout_deliveryAddress_til);
        dialog_items_rv = cart_dialog.findViewById(R.id.cart_layout_itemList_rv);
        dialog_items_rv.setLayoutManager(new LinearLayoutManager(this));
        dialog_items_rv.setHasFixedSize(false);

        dialog_cart_adapter = new CartItemAdapter(newCart.getItemList(), InventoryActivity.this, newCart.getQuantity());
        dialog_cart_adapter.setClickListener(cartListener);

//        dialog_cart_adapter.setClickListener((item, pos, view) -> {
//            if(pos==-1){
//                cart.clear();
//                cart_fab.setText(String.format("%s", cart.size()));
//                cart_dialog.dismiss();
//            }else if(pos==-2){
//                cart.clear();
//                cart = dialog_cart_adapter.getItemList();
//                cart_fab.setText(String.format("%s", cart.size()));
//            }
//        });

        dialog_items_rv.setAdapter(dialog_cart_adapter);

        dialog_confirm_bt.setOnClickListener(v -> {
            String address = Objects.requireNonNull(delivery_address_til.getEditText()).getText().toString().trim();
            if(address.equals("")){
                delivery_address_til.setError("Address Can't be empty");
            } else{
                delivery_address_til.setErrorEnabled(false);
                delivery_address = address;
                showInvoice();
            }
        });
        cart_dialog.setOnShowListener(dialog -> {

        });
        cart_dialog.show();
    }

    void showInvoice(){
        mInvoice = Invoice.fromCart(newCart);
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
        Window window = invoice_dialog.getWindow();
        assert window!=null;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

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


        payAndOrder_bt.setOnClickListener(v -> {
            if(item_count_tv.getText().equals(Invoice.STR_ITEM_COUNT+" 0")) {
                Toast.makeText(InventoryActivity.this, "There are no items in the cart. ", Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(InventoryActivity.this)
                        .setTitle("Proceed Further")
                        .setMessage("Are you sure? Money will be automatically deducted from your SabPay wallet ")

                        .setPositiveButton("Continue", (dialog, which) -> {
                            invoice_dialog.dismiss();
                            pay();
                        })

                        .setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        confirmOrder_bt.setOnClickListener(v -> {
            if(item_count_tv.getText().equals(Invoice.STR_ITEM_COUNT+" 0")) {
                Toast.makeText(InventoryActivity.this, "There are no items in the cart. ", Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(InventoryActivity.this)
                        .setTitle("Proceed Further")
                        .setMessage("Are you sure?")

                        .setPositiveButton("Continue", (dialog, which) -> {
                            invoice_dialog.dismiss();
                            placeOrder(null);
                        })

                        .setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

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
        String item_count = Invoice.STR_ITEM_COUNT+" "+newCart.getUniqueItemCount();
        String entity_count = Invoice.STR_ENTITY_COUNT+" "+newCart.getItemCount();
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
        confirmOrder_bt.setEnabled(false);
        payAndOrder_bt.setEnabled(false);
        cart_dialog_pb.setVisibility(View.VISIBLE);
        Call<Map<String, Object>> pay = API.getApiService().pay(mAuth.getUid(),
               mInventory.getOwner().getId(), mInvoice.getTotal_amount(), API.api_key);
        pay.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NotNull Call<Map<String, Object>> call,
                                   @NotNull Response<Map<String, Object>> response) {
                confirmOrder_bt.setEnabled(true);
                payAndOrder_bt.setEnabled(true);
                cart_dialog_pb.setVisibility(View.GONE);
                Map<String, Object> result;
                if(!response.isSuccessful()){
                    String body;
                    try {
                        assert response.errorBody() != null;
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
                assert result != null;
                if(result.containsKey("error")){
                    Utils.toast(InventoryActivity.this,
                            Objects.requireNonNull(result.get("error")).toString(), Toast.LENGTH_LONG);
                }else{
                    String s = String.format("Status: %s\nFrom: %s\nAmount: %s\nTransaction Id: %s",
                            result.get("status"), result.get("from"), result.get("amount"), result.get("transactionId"));
                    Utils.toast(InventoryActivity.this, s, Toast.LENGTH_LONG);
                    String transactionId =  Objects.requireNonNull(result.get("transactionId")).toString();
                    placeOrder(transactionId);
                }
            }

            @Override
            public void onFailure(@NotNull Call<Map<String, Object>> call, @NotNull Throwable t) {
                confirmOrder_bt.setEnabled(true);
                payAndOrder_bt.setEnabled(true);
                cart_dialog_pb.setVisibility(View.GONE);
                //Log.i("test", "onFailure: "+t.getLocalizedMessage());
                Utils.toast(InventoryActivity.this, t.getLocalizedMessage(), Toast.LENGTH_LONG);
            }
        });
    }

    void placeOrder(String transactionId){
        confirmOrder_bt.setEnabled(false);
        payAndOrder_bt.setEnabled(false);
        cart_dialog_pb.setVisibility(View.VISIBLE);
        Order order = new Order();
        order.setOrderId(mRef.collection("order").document().getId());
        order.setAmount(mInvoice.getTotal_amount());
        order.setActive(false);
        order.setFromInventory(mInventory.getId());
        order.setFromInventoryName(mInventory.getName());
        User user = Paper.book("user").read("user");
        order.setUser(new HashMap<String, String>(){{
            put("userId", user.getUid());
            put("firstName", user.getFirstName());
            put("lastName", user.getLastName());
            put("phone", user.getPhone());
            put("address", delivery_address);
        }});
        order.setTimestamp(new Timestamp(new Date()));
        order.setStatus(Order.STATUS.ORDER_RECEIVED);
        List<Item> it = new ArrayList<>(mInvoice.getItems());
        for(Item i:it){
            i.setQty(i.getCart_qty());
            if(i.getQty()==0){
                it.remove(i);
            }
        }
        order.setItems(it);
        if(transactionId==null){
            order.setTransactionId(null);
            order.setInvoiceId(null);
        }else{
            String invoiceId = mRef.collection(String.format("user/%s/invoice", mAuth.getUid())).document().getId();
            order.setTransactionId(transactionId);
            order.setInvoiceId(invoiceId);
            mInvoice.setId(invoiceId);
            mInvoice.setPromo(null);
            mInvoice.setTimestamp(new Timestamp(new Date()));
            mInvoice.setTransaction(transactionId);
        }
        order.updateActiveState();
        mRef.runTransaction((Transaction.Function<Void>) transaction -> {
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
        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Utils.toast(InventoryActivity.this, "Order Placed Successfully", Toast.LENGTH_LONG);
                String msg = String.format("Address: %s\nOrder Id: %s\nAmount: Rs. %s\nFrom: %s",
                        delivery_address,
                        order.getOrderId(),
                        order.getAmount(),
                        order.getUser().get("firstName"));
                new SabPayNotify.Builder()
                        .setTitle("New Order")
                        .setMessage(msg)
                        .send(getApplicationContext(), mInventory.getOwner().getId(), true);
            }else{
                //Log.i("test", "onComplete: "+task.getException().getLocalizedMessage());
                Utils.toast(InventoryActivity.this,
                        Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_LONG);
            }
            cart_dialog_pb.setVisibility(View.GONE);
            new Handler().postDelayed(() -> {
                cart_dialog.dismiss();
                finish();
            }, 1500);

        });
    }

    private void revealShow(View rootView, boolean reveal, final AlertDialog dialog){
        final View view = rootView.findViewById(R.id.reveal_view);
        int w = view.getWidth();
        int h = view.getHeight();
        float maxRadius = (float) Math.sqrt(w * w / 4 + h * h / 4);

        if(reveal){
            Animator revealAnimator = ViewAnimationUtils.createCircularReveal(view,
                    w / 2, h / 2, 0, maxRadius);

            view.setVisibility(View.VISIBLE);
            revealAnimator.start();

        } else {

            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, w / 2, h / 2, maxRadius, 0);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    dialog.dismiss();
                    view.setVisibility(View.INVISIBLE);

                }
            });

            anim.start();
        }

    }

}