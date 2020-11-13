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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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
import com.visionio.sabpay.models.CompressedItem;
import com.visionio.sabpay.models.Inventory;
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

public class InventoryActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    Inventory mInventory;

    AppBarLayout appBarLayout;

    MaterialToolbar toolbar;
    EditText search;
    ImageView searchClose;
    RecyclerView searchRecycler;
    SearchListAdapter searchListAdapter;

    View sep_layer_view;

    InventoryItemAdapter adapter;
    RecyclerView recyclerView;
    NestedScrollView nestedScrollView;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    ExtendedFloatingActionButton cart_fab;

    ProgressBar progressBar;

    TextView shop_name;

    // pagination query
    int itemLimit = 10;
    boolean isAllItemsLoaded = false;

    // dialog views
    Dialog cart_dialog;
    ProgressBar cart_dialog_pb;
    TextInputLayout delivery_address_til, delivery_address_til_1;
    String delivery_address;
    Button dialog_pay_order_bt, dialog_cod_order_bt;
    RecyclerView dialog_items_rv;
    CartItemAdapter dialog_cart_adapter;

    ChipGroup categoryFilter;

    String tag;
    List<String> tagsList;
    Boolean filterChanged = false;

    List<String> searchList;


    Spinner spinner;

    CartListener cartListener = new CartListener() {
        @Override
        public void onIncreaseQty(Item item) {
            Cart.getInstance().addItem(item);
            cart_fab.setText(String.format("%s", Cart.getInstance().getItemCount()));
        }

        @Override
        public void onDecreaseQty(Item item) {
            Cart.getInstance().decreaseItem(item);
            cart_fab.setText(String.format("%s", Cart.getInstance().getItemCount()));
            if (dialog_cart_adapter != null) {
                dialog_cart_adapter.notifyDataSetChanged();
                if(dialog_cart_adapter.getItemCount()==0){
                    cart_dialog.dismiss();
                }
            }
        }
    };

    // state management objects
    boolean isLoading = false;
    boolean isSearchQueryLoaded = false; // use to check if searchString is already loaded to avoid multiple calls
    boolean isSearchQueryLoading = false; // use to block server req until previous is finished

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

    public String getCompoundTag(List<String> tags) {
        StringBuilder builder = new StringBuilder();
        for(String tag: tags){
            builder.append(String.format("%s+", tag));
        }
        builder.deleteCharAt(builder.length()-1);
        return builder.toString();
    }

    private void setup() {
        Bundle bundle = this.getIntent().getExtras();
        assert bundle != null;
        tagsList = bundle.getStringArrayList("tags");
        String header = bundle.getString("header");

        String json = Paper.book().read("json");
        Log.d("testing", "setup: " + json);

        mInventory = Inventory.formJson(json);

        Objects.requireNonNull(getSupportActionBar()).setTitle(header);

        tag = getCompoundTag(tagsList);

        searchRecycler = findViewById(R.id.inventory_search_recycler);
        searchClose = findViewById(R.id.inventory_image_close);
        sep_layer_view = findViewById(R.id.inventory_separator_layer_view);
        search = findViewById(R.id.inventory_search_et);
        toolbar = findViewById(R.id.inv_activity_toolbar);
        progressBar = findViewById(R.id.inv_activity_progress);
        nestedScrollView = findViewById(R.id.inv_activity_rv_nestedScrollView);
        cart_fab = findViewById(R.id.inv_activity_cart_exFab);
        recyclerView = findViewById(R.id.inv_activity_rv);
        spinner = findViewById(R.id.inventory_activity_spinner);
        shop_name = findViewById(R.id.inventory_activity_shop_name);
        shop_name.setText(header);
        List<String> list = new ArrayList<>();
        list.add("All");
        for(String s : tagsList) {
            s = s.replace("_", " ");
            s = s.replace("1", "-");
            s = s.replace("2", "&");
            s = s.replace("3", ",");
            s = s.replace("4", ".");
            list.add(s);
        }
        cart_fab.setText(String.format("%s", Cart.getInstance().getItemCount()));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();

        spinner.setOnItemSelectedListener(this);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        toolbar.bringToFront();

        appBarLayout = findViewById(R.id.inv_activity_app_bar_layout);


        adapter = new InventoryItemAdapter(this, new ArrayList<>(), mInventory.getId());
        searchListAdapter = new SearchListAdapter(new ArrayList<>(), (object, position, view) -> {
            for (Item it : Cart.getInstance().getItemList()) {
                if (it.getId().equals(object)) {
                    Cart.getInstance().addItem(it);
                    cart_fab.setText(String.format("%s", Cart.getInstance().getItemCount()));
                    Utils.toast(InventoryActivity.this, String.format("%s already in cart", it.getTitle()), Toast.LENGTH_SHORT);
                    return;
                }
            }
            FirebaseFirestore.getInstance().collection("item").document(object)
                    .get().addOnSuccessListener(snapshot -> {
                Item item = snapshot.toObject(Item.class);
                assert item != null;
                Cart.getInstance().addItem(item);
                cart_fab.setText(String.format("%s", Cart.getInstance().getItemCount()));
            });
        });


        searchRecycler.setLayoutManager(new LinearLayoutManager(this));
        searchRecycler.setHasFixedSize(false);
        searchRecycler.setAdapter(searchListAdapter);

        adapter.setClickListener(cartListener);

        recyclerView.setAdapter(adapter);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.inventory_appbar_info){
                showInfoDialog();
                return true;
            }
            else if (item.getItemId() == R.id.inventory_appbar_search) {
                loadSearchString();
                return true;
            }
            return false;
        });

        searchClose.setOnClickListener(v -> {
            searchClose.setVisibility(View.GONE);
            shop_name.setVisibility(View.VISIBLE);
            search.setVisibility(View.GONE);
            searchRecycler.setVisibility(View.GONE);
            sep_layer_view.setVisibility(View.GONE);
            searchListAdapter.clear();
            search.setText("");
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}  @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) {
                searchListAdapter.clear();
                for(String s : searchList) {
                    if (s.toLowerCase().contains(editable.toString().toLowerCase()) && !editable.toString().isEmpty()) {
                        searchListAdapter.add(s);
                    } else if (editable.toString().isEmpty()) {
                        searchListAdapter.clear();
                    }
                }
            }
        });

        cart_fab.setOnClickListener(v -> {
            if(Cart.getInstance().getItemCount() == 0){
                Utils.toast(InventoryActivity.this, "No items in cart", Toast.LENGTH_SHORT);
                return;
            }
            showCart();
        });

        nestedScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    View view = nestedScrollView.getChildAt(nestedScrollView.getChildCount() - 1);
                    int diff = (view.getBottom() - (nestedScrollView.getHeight() + nestedScrollView.getScrollY()));
                    if (diff == 0) {
                        loadItems();
                        Toast.makeText(InventoryActivity.this, "Loading more items", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        loadItems();
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

    void loadItems(){
        if (isLoading){
            Utils.toast(this, "Result already loading", Toast.LENGTH_LONG);
            return;
        }
        if(isAllItemsLoaded && !filterChanged){
            Utils.toast(this, "No more items", Toast.LENGTH_LONG);
            isLoading = false;
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        isLoading = true;
        String lastTitle = filterChanged ? "+" : adapter.get_last_title();
        Call<Map<String, Object>> getItemsCall = API.getApiService().getItems(lastTitle, tag, itemLimit);
        getItemsCall.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading = false;
                if(response.code()==200){
                    Map<String, Object> map = response.body();
                    List<Objects> items = (List<Objects>)map.get("data");
                    List<Item> res = new ArrayList<>();
                    for(Object items_obj: items){
                        String item_js = new Gson().toJson(items_obj);
                        Item item = new Gson().fromJson(item_js, Item.class);
                        res.add(item);
                    }
                    if(res.size()==0){
                        isAllItemsLoaded = true;
                    }
                    if(filterChanged) adapter.setItemListNull();
                    adapter.setItemList(res);
                    filterChanged = false;
                }else{
                    filterChanged = false;
                    String error = response.errorBody().toString();
                    Utils.toast(InventoryActivity.this, error, Toast.LENGTH_LONG);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading = false;
                Log.i("inventory_activity", "onFailure: "+t.getLocalizedMessage());
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    void loadSearchString(){
        if(isSearchQueryLoading){
            Utils.toast(getApplicationContext(), "Loading...!", Toast.LENGTH_LONG);
            return;
        }
        shop_name.setVisibility(View.GONE);
        if(isSearchQueryLoaded){
            searchClose.setVisibility(View.VISIBLE);
            shop_name.setVisibility(View.GONE);
            search.setVisibility(View.VISIBLE);
            searchRecycler.setVisibility(View.VISIBLE);
            sep_layer_view.setVisibility(View.VISIBLE);
            search.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        isSearchQueryLoading = true;
        mRef.runTransaction(transaction -> {
            List<String> search_string = new ArrayList<>();
            CollectionReference collectionReference = mRef.collection("search");
            for(String tag: tagsList){
                DocumentReference ref = collectionReference.document(tag);
                DocumentSnapshot snap = transaction.get(ref);
                if (snap.contains("query_string"))
                    search_string.addAll((List<String>) Objects.requireNonNull(snap.get("query_string")));
            }
            return search_string;
        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                List<String> strings = task.getResult();
                assert strings!=null;
                searchList.addAll(strings);
                searchClose.setVisibility(View.VISIBLE);
                shop_name.setVisibility(View.GONE);
                search.setVisibility(View.VISIBLE);
                searchRecycler.setVisibility(View.VISIBLE);
                sep_layer_view.setVisibility(View.VISIBLE);
                search.requestFocus();
                isSearchQueryLoaded = true;
            }else {
                Utils.toast(getApplicationContext(),
                        task.getException().toString(), Toast.LENGTH_LONG);
            }
            isSearchQueryLoading = false;
            progressBar.setVisibility(View.GONE);
        });
    }

    void showCart(){
        if(cart_dialog!=null){
            dialog_cart_adapter.setItemList(Cart.getInstance().getItemList());
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
        dialog_pay_order_bt = cart_dialog.findViewById(R.id.cart_layout_pay_order_bt);
        dialog_cod_order_bt = cart_dialog.findViewById(R.id.cart_layout_cod_order_bt);
        delivery_address_til = cart_dialog.findViewById(R.id.cart_layout_deliveryAddress_til);
        delivery_address_til_1 = cart_dialog.findViewById(R.id.cart_layout_deliveryAddress_til_1);
        dialog_items_rv = cart_dialog.findViewById(R.id.cart_layout_itemList_rv);
        dialog_items_rv.setLayoutManager(new LinearLayoutManager(this));
        dialog_items_rv.setHasFixedSize(false);

        dialog_cart_adapter = new CartItemAdapter(Cart.getInstance().getItemList(), InventoryActivity.this, Cart.getInstance().getQuantity(), mInventory.getId());
        dialog_cart_adapter.setClickListener(cartListener);

        dialog_items_rv.setAdapter(dialog_cart_adapter);

        dialog_cod_order_bt.setOnClickListener(v -> {
            String address = Objects.requireNonNull(delivery_address_til.getEditText()).getText().toString().trim();
            String address1 = Objects.requireNonNull(delivery_address_til_1.getEditText()).getText().toString().trim();
            if(address.equals("")){
                delivery_address_til.setError("Flat Number Can't be empty");
            }
            if (address1.equals("")) {
                delivery_address_til_1.setError("Building Number Can't be empty");
            }
            if(!address.equals("")){
                delivery_address_til.setErrorEnabled(false);
            }
            if(!address1.equals("")) {
                delivery_address_til_1.setErrorEnabled(false);
            }
            if(!address.equals("") && !address1.equals("")) {
                delivery_address = "Flat: " + address +", Building: " +  address1;
                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(InventoryActivity.this);
                alert.setTitle("Order Confirmation");
                alert.setMessage("Total Items: " + String.valueOf(Cart.getInstance().getItemCount()) + "\nTotal Amount: " + String.valueOf(Cart.getInstance().getAmount()));
                alert.setPositiveButton("Yes", (dialog, which) -> {
                    placeOrder(null);
                });
                alert.setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                });
                alert.show();
            }
        });
        dialog_pay_order_bt.setOnClickListener(v -> {
            String address = Objects.requireNonNull(delivery_address_til.getEditText()).getText().toString().trim();
            if(address.equals("")){
                delivery_address_til.setError("Address Can't be empty");
            } else{
                delivery_address_til.setErrorEnabled(false);
                delivery_address = address;
                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(InventoryActivity.this);
                alert.setTitle("Order Confirmation");
                alert.setMessage("Total Items: " + String.valueOf(Cart.getInstance().getItemCount()) + "\nTotal Amount: " + String.valueOf(Cart.getInstance().getAmount()));
                alert.setPositiveButton("Yes", (dialog, which) -> {
                    pay();
                });
                alert.setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                });
                alert.show();
            }
        });
        cart_dialog.setOnShowListener(dialog -> {
        });
        cart_dialog.show();
    }

    void pay(){
        dialog_pay_order_bt.setEnabled(false);
        dialog_cod_order_bt.setEnabled(false);
        cart_dialog_pb.setVisibility(View.VISIBLE);
        Call<Map<String, Object>> pay = API.getApiService().pay(mAuth.getUid(),
               mInventory.getOwner().getId(), Cart.getInstance().getAmount(), API.api_key);
        pay.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NotNull Call<Map<String, Object>> call,
                                   @NotNull Response<Map<String, Object>> response) {
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
                dialog_cod_order_bt.setEnabled(true);
                dialog_pay_order_bt.setEnabled(true);
                cart_dialog_pb.setVisibility(View.GONE);
                //Log.i("test", "onFailure: "+t.getLocalizedMessage());
                Utils.toast(InventoryActivity.this, t.getLocalizedMessage(), Toast.LENGTH_LONG);
            }
        });
    }

    void placeOrder(String transactionId){
        dialog_cod_order_bt.setEnabled(false);
        dialog_pay_order_bt.setEnabled(false);
        cart_dialog_pb.setVisibility(View.VISIBLE);
        Order order = new Order();
        order.setOrderId(mRef.collection("order").document().getId());
        order.setAmount(Cart.getInstance().getAmount());
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
        List<CompressedItem> it = Cart.getInstance().getCompressedItem();
        order.setItems(it);
        if(transactionId==null){
            order.setTransactionId(null);
            order.setInvoiceId(null);
        }else{
            String invoiceId = mRef.collection(String.format("user/%s/invoice", mAuth.getUid())).document().getId();
            order.setTransactionId(transactionId);
            order.setInvoiceId(invoiceId);
        }
        order.updateActiveState();
        mRef.document(String.format("order/%s", order.getOrderId())).set(order).addOnCompleteListener(task -> {
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
                Cart.getInstance().clear();
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

    @Override
    public void onBackPressed() {
        if(sep_layer_view.getVisibility() == View.VISIBLE){
            searchClose.setVisibility(View.GONE);
            search.setVisibility(View.GONE);
            searchRecycler.setVisibility(View.GONE);
            sep_layer_view.setVisibility(View.GONE);
            search.requestFocus();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(String.valueOf(parent.getSelectedItem()) == "All") {
            tag = getCompoundTag(tagsList);
        } else {
            tag = String.valueOf(parent.getSelectedItem());
            tag = tag.replace(" ", "_");
            tag = tag.replace("-", "1");
            tag = tag.replace("&", "2");
            tag = tag.replace(",", "3");
            tag = tag.replace(".", "4");
        }
        filterChanged = true;
        loadItems();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}