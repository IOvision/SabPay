package com.visionio.sabpay.main;


import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.CartItemAdapter;
import com.visionio.sabpay.api.SabPayNotify;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.group_pay.pending.PendingPaymentActivity;
import com.visionio.sabpay.helper.TokenManager;
import com.visionio.sabpay.interfaces.CartListener;
import com.visionio.sabpay.models.Cart;
import com.visionio.sabpay.models.CompressedItem;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.Inventory;
import com.visionio.sabpay.models.Item;
import com.visionio.sabpay.models.Order;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity{

    TextView textCartItemCount;
    int mCartItemCount = Cart.getInstance().getItemCount();

    Inventory mInventory;
    FragmentManager fragmentManager;
    FrameLayout frameLayout;
    FragmentTransaction fragmentTransaction;
    MaterialToolbar materialToolbar;
    BottomNavigationView bottomNavigationView;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    boolean isContactLoaded = false;
    private BroadcastReceiver mMessageReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Alert", "Creating Alert");
            showAlert(intent);
        }
    };

    //Cart
    Dialog cart_dialog;
    ProgressBar cart_dialog_pb;
    TextInputLayout delivery_address_til, delivery_address_til_1;
    String delivery_address;
    Button dialog_pay_order_bt, dialog_cod_order_bt;
    RecyclerView dialog_items_rv;
    CartItemAdapter dialog_cart_adapter;

    CartListener cartListener = new CartListener() {
        @Override
        public void onIncreaseQty(Item item) {
            Cart.getInstance().addItem(item);
            mCartItemCount = Cart.getInstance().getItemCount();
            setUpBadge();
        }

        @Override
        public void onDecreaseQty(Item item) {
            Cart.getInstance().decreaseItem(item);
            if (dialog_cart_adapter != null) {
                dialog_cart_adapter.notifyDataSetChanged();
                if(dialog_cart_adapter.getItemCount()==0){
                    cart_dialog.dismiss();
                }
            }
            mCartItemCount = Cart.getInstance().getItemCount();
            setUpBadge();
        }
    };

    private void showAlert(Intent intent) {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
        alert.setTitle(intent.getStringExtra("value1"));
        alert.setMessage(intent.getStringExtra("value2"));
        alert.setPositiveButton("Ok", (dialog, which) -> {
            dialog.dismiss();
        });
        alert.show();
    }

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mAuth.getUid() != null) {
            setUp();
        } else {
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if(bottomNavigationView.getSelectedItemId() == R.id.bottom_app_bar_main_offers) {
            super.onBackPressed();
        } else {
            bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_offers);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_offers);
        mCartItemCount = Cart.getInstance().getItemCount();
        setUpBadge();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReciever,
                new IntentFilter("myFunction"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReciever);
    }

    void setUp() {
        TokenManager.handle(this);

        frameLayout = findViewById(R.id.main_frame);
        bottomNavigationView = findViewById(R.id.main_bottom_navigation);
        materialToolbar = findViewById(R.id.main_top_bar);

        materialToolbar.inflateMenu(R.menu.top_app_bar);
        Menu menu = materialToolbar.getMenu();
        final MenuItem menuCart = menu.findItem(R.id.cart);
        final MenuItem menuLogout = menu.findItem(R.id.logout);
        menuCart.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (Cart.getInstance().getItemCount() == 0) {
                    Utils.toast(MainActivity.this, "No items in cart", Toast.LENGTH_SHORT);
                    return true;
                }
                showCart();
                Log.d("testing", "onMenuItemClick: hello");
                return true;
            }
        });
        menuLogout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                signOut();
                return true;
            }
        });
        View actionView = menuCart.getActionView();
        textCartItemCount = actionView.findViewById(R.id.cart_badge);
        textCartItemCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Cart.getInstance().getItemCount() == 0) {
                    Utils.toast(MainActivity.this, "No items in cart", Toast.LENGTH_SHORT);
                }
                showCart();
                Log.d("testing", "onMenuItemClick: hello");
            }
        });
        setUpBadge();

        mInventory = Inventory.formJson(Paper.book().read("json"));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {

//            if (item.getItemId() == R.id.bottom_app_bar_main_group){
//                groupPay();
//            } else if (item.getItemId() == R.id.bottom_app_bar_main_home){
//                //offers();
//                home();
//            } else if (item.getItemId() == R.id.bottom_app_bar_main_pay){
//                if(!isContactLoaded){
//                  Toast.makeText(MainActivity.this, "Contact still loading", Toast.LENGTH_SHORT).show();
//                }else{
//                    pay();
//                }
//            } else
            if (item.getItemId() == R.id.bottom_app_bar_main_transaction){
                transactionHistory();
            } else if (item.getItemId() == R.id.bottom_app_bar_main_offers){
                offers();
            }
            return true;
        });
//        loadContacts();
    }

    private void setUpBadge() {
        if (textCartItemCount != null) {
            if (mCartItemCount == 0) {
                if (textCartItemCount.getVisibility() != View.GONE) {
                    textCartItemCount.setVisibility(View.GONE);
                }
            } else {
                textCartItemCount.setText(String.valueOf(Math.min(mCartItemCount, 99)));
                if (textCartItemCount.getVisibility() != View.VISIBLE) {
                    textCartItemCount.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void offers() {
        InventoryFragment fragment = new InventoryFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void transactionHistory() {
        TransactionHistoryFragment fragment = new TransactionHistoryFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void groupPay() {
        GroupPayFragment fragment = new GroupPayFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void pay() {
        PayFragment fragment = new PayFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void home() {
        materialToolbar.setTitle("Home");
        HomeFragment fragment = new HomeFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    void signOut() {
        for(ListenerRegistration registration: Utils.registrations){
            registration.remove();
        }
        mRef.collection("user").document(mAuth.getUid()).update(new HashMap<String, Object>(){{
            //put("login", false);
            put("instanceId", null);
        }})
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mAuth.signOut();
                        Intent intent = new Intent(getApplicationContext(), AuthenticationActivity.class);
                        startActivity(intent);
                        finishAffinity();
                    } else {
                        Toast.makeText(MainActivity.this, "Could not sign out", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void setTitle(String title){
        materialToolbar.setTitle(title);
    }

//    List<Contact> getAllLocalContacts(){
//            List<Contact> contacts = new ArrayList<>();
//            Cursor phones = getApplicationContext().getContentResolver().query(
//                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
//                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
//            while (phones.moveToNext()) {
//                String id = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
//                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
//                String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//
//                Contact contact = new Contact(id, name, phoneNumber);
//                contacts.add(contact);
//            }
//            return contacts;
//    }

//    private List<String> getNumberArray(List<Contact> contacts){
//        List<String> numbers = new ArrayList<>();
//        for(Contact c: contacts){ if (!numbers.contains(c.getNumber())) numbers.add(c.getNumber()); }
//        return numbers;
//    }
//
//    private void loadContacts(){
//        Paper.book().delete("contacts");
//        if (Paper.book().contains("contacts")){
//            Utils.deviceContacts = Paper.book().read("contacts");
//        } else {
//            final List<Contact> contactList = new ArrayList<>();
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
//                    checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
//                //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
//            } else {
//                // Android version is lesser than 6.0 or the permission is already granted.
//                Toast.makeText(this, "loading contacts.", Toast.LENGTH_SHORT).show();
//                List<Contact> allContacts = getAllLocalContacts();
//                List<String> numbers = getNumberArray(allContacts);
//
//                if(numbers.size()==0){
//                    saveContact(new ArrayList<>());
//                    return;
//                }
//                loadingContactsServerCall(allContacts);
//            }
//        }
//    }
//    void loadingContactsServerCall(List<Contact> deviceContacts){
//        Toast.makeText(this, "loading contacts.", Toast.LENGTH_SHORT).show();
//        List<String> stringNumbers = getNumberArray(deviceContacts);
//        if(stringNumbers.size() == 0){
//            saveContact(new ArrayList<>());
//            return;
//        }
//        mRef.document("/public/registeredPhone").get()
//                .addOnCompleteListener(task -> {
//                    if (!task.isSuccessful()){
//                        Utils.toast(MainActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_LONG);
//                        saveContact(new ArrayList<>());
//                        return;
//                    }
//                    @SuppressWarnings("unchecked")
//                    List<String> numberArrayFromServer = (List<String>) task.getResult().get("number");
//                    if(numberArrayFromServer == null){
//                        saveContact(new ArrayList<>());
//                        return;
//                    }
//                    List<Contact> commonContacts = new ArrayList<>();
//                    for(String sc: numberArrayFromServer){
//                        for(Contact contact: deviceContacts){
//                            if(contact.getNumber().equalsIgnoreCase(sc)){
//                                commonContacts.add(contact);
//                            }
//                        }
//                    }
//                    saveContact(commonContacts);
//                });
//    }
//
//    void saveContact(List<Contact> contacts){
//        isContactLoaded = true;
//        Paper.book().write("contacts", contacts);
//    }

    void startPendingPayment(){
        startActivityForResult(new Intent(MainActivity.this, PendingPaymentActivity.class), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1){
//            bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_group);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void showCart(){
        if(cart_dialog != null){
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

        dialog_cart_adapter = new CartItemAdapter(Cart.getInstance().getItemList(), this, Cart.getInstance().getQuantity(), mInventory.getId());
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
                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
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
                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
                alert.setTitle("Order Confirmation");
                alert.setMessage("Total Items: " + String.valueOf(Cart.getInstance().getItemCount()) + "\nTotal Amount: " + String.valueOf(Cart.getInstance().getAmount()));
                alert.setPositiveButton("Yes", (dialog, which) -> {
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
            String invoiceId = mRef.collection(String.format("user/%s/invoice", FirebaseAuth.getInstance().getUid())).document().getId();
            order.setTransactionId(transactionId);
            order.setInvoiceId(invoiceId);
        }
        order.updateActiveState();
        mRef.document(String.format("order/%s", order.getOrderId())).set(order).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Utils.toast(this, "Order Placed Successfully", Toast.LENGTH_LONG);
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
                Utils.toast(this,
                        Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_LONG);
            }
            cart_dialog_pb.setVisibility(View.GONE);
            new Handler().postDelayed(() -> {
                Cart.getInstance().clear();
                cart_dialog.dismiss();
            }, 1500);
        });
    }
}