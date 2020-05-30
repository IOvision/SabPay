package com.visionio.sabpay.main;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.Result;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.ContactAdapter;
import com.visionio.sabpay.helper.GroupSelectHandler;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.interfaces.Payment;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.GroupPay;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.payment.PaymentActivity;

import java.util.ArrayList;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class PayFragment extends Fragment {

    public PayFragment() {
        // Required empty public constructor
    }

    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    EditText et_number;
    RecyclerView recyclerView;

    LoadContacts a;
    ImageView overlay;
    ProgressBar progressBar;

    ContactAdapter adapter;
    ViewGroup contentFrame;
    ZXingScannerView mScannerView;
    Boolean scannerOpen = true;


    String jsonFromQr;

    String phoneNumber;
    DocumentReference senderDocRef, receiverDocRef;

    Button pay;

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    static final int FLAG_MODE_DIRECT_PAY = -1;
    static final int FLAG_MODE_SEARCH_AND_PAY = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay, container, false);

        et_number = view.findViewById(R.id.pay_number);
        pay = view.findViewById(R.id.pay_btn_pay);
        recyclerView = view.findViewById(R.id.pay_fragment_recycler);

        overlay = view.findViewById(R.id.overlay);
        progressBar = view.findViewById(R.id.pay_progress);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.HORIZONTAL));

        adapter = new ContactAdapter(getContext(), new ArrayList<Contact>(), new ArrayList<Contact>());
        adapter.setClickListener(new OnItemClickListener<Contact>() {
            @Override
            public void onItemClicked(Contact contact, int position, View v) {
                initiateServer(FLAG_MODE_DIRECT_PAY, contact);
            }

        });

        recyclerView.setAdapter(adapter);
        a = new LoadContacts();
        a.execute(adapter);

        pay.setOnClickListener(v -> {
            if (et_number.getText().toString().isEmpty()){
                et_number.setError("Cannot be empty!");
            } else {
                initiateServer(FLAG_MODE_SEARCH_AND_PAY, null);
                overlay.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        et_number.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    hideScanner();
                    showContacts();
                }else {
                    showScanner();
                }
            }
        });

        et_number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length()<1){
                    showScanner();
                    hideKeyboard(view);
                }
                adapter.applyFilter(s.toString().trim().toLowerCase());
            }
        });

        setUp(view);
        return view;
    }

    void setUp(View view){
        contentFrame = (ViewGroup) view.findViewById(R.id.pay_fragment_frame);
        mScannerView = new ZXingScannerView(getContext());
        mScannerView.setResultHandler(new ZXingScannerView.ResultHandler() {
            @Override
            public void handleResult(Result rawResult) {
                String res = rawResult.getText();
                if(Utils.getPaymentType(res)==0){
                    et_number.setText(res);
                    initiateServer(FLAG_MODE_SEARCH_AND_PAY, null);
                }else{
                    jsonFromQr = res;
                    searchGroupOwner();
                }

                Log.i("Testing", "Payment type: "+Utils.getPaymentType(res));

            }
        });
        contentFrame.addView(mScannerView);
        startCamera();
        senderDocRef = mRef.collection("user").document(mAuth.getUid());
    }

    private void startCamera(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                getContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
           requestPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        }else{
            mScannerView.startCamera(0);
        }
    }

    private void requestPermission(String permission, int code){
        if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)){

        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, code);
        }
    }


    void searchUser(){
        mRef.collection("user").whereEqualTo("phone", phoneNumber).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            if(!task.getResult().getDocuments().isEmpty()){
                                DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                                String name = snapshot.getString("name");
                                receiverDocRef = snapshot.getReference();
                                Payment.createInstance(receiverDocRef,name);
                                startActivity(new Intent(getActivity(), PaymentActivity.class));
                            }else{
                                /*paymentHandler.showPayStatus();
                                paymentHandler.setError("No wallet linked to this number!!");*/
                                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(getContext());
                                alert.setTitle("No Wallet Found.").setMessage("No Wallet is linked to this Number.")
                                        .setPositiveButton("Okay", (dialog, which) -> {
                                            dialog.dismiss();
                                        });
                            }
                        }else{
                            //
                        }
                    }
                });
    }

    void initiateServer(int flag, Contact contact){
        // info: if flag is @FLAG_MODE_SEARCH_AND_PAY then contact will be null
        if(flag == FLAG_MODE_SEARCH_AND_PAY){
            updateVariableData();
            searchUser();
        }else{
            directPay(contact);
        }
    }

    void updateVariableData(){
        phoneNumber = Utils.formatNumber(et_number.getText().toString().trim(), 0);
    }
    void directPay(Contact contact){
        Payment.createInstance(receiverDocRef, contact.getUser().getName());
        startActivity(new Intent(getActivity(), PaymentActivity.class));
    }

    private void hideScanner(){
        scannerOpen = false;
        mScannerView.stopCamera();
        contentFrame.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showScanner(){
        scannerOpen = true;
        recyclerView.setVisibility(View.GONE);

        mScannerView.startCamera();
        contentFrame.setVisibility(View.VISIBLE);
    }

    private void showContacts(){


    }

    private void addIfContactIsRegistered(final Contact contact){
         /*this function checks if contact from local mobile is registered with our app or not
        * if yes then we add it to adapter else do nothing
        *
                \*/
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View v = getActivity().getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (v == null) {
            v = new View(getContext());
        }
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    void searchGroupOwner(){
        String[] groupPayData = Utils.getUserIdFromGpayId(jsonFromQr);
        receiverDocRef = mRef.document("user/"+groupPayData[0]);

        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Getting details");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        receiverDocRef.collection("group_pay")
                .document("meta-data/transaction/"+groupPayData[1]).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            progressDialog.dismiss();
                            GroupPay groupPay = task.getResult().toObject(GroupPay.class);
                            splitIntoGroup(groupPay);
                        }else{
                            progressDialog.dismiss();
                            Log.i("Testing", task.getException().getLocalizedMessage());
                        }
                    }
                });
    }

    void splitIntoGroup(final GroupPay groupPay){
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), "Canceled", Toast.LENGTH_SHORT).show();
            }
        };

        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final GroupSelectHandler groupSelectHandler = new GroupSelectHandler(getActivity(), groupPay);
            }
        };

        final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("Confirm?")
                .setNegativeButton("Cancel", negativeListener)
                .setPositiveButton("Yes", positiveListener)
                .create();

        receiverDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    User user = task.getResult().toObject(User.class);
                    alertDialog.setMessage("Name: "+user.getName()+"\nTotal Amount: "+groupPay.getAmount());
                    alertDialog.show();
                }else {
                    Log.i("Testing", task.getException().getLocalizedMessage());
                }
            }
        });
    }

    public final class LoadContacts extends AsyncTask<ContactAdapter, Void, Void> {

        boolean active=true;
        @Override
        protected Void doInBackground(ContactAdapter... contactAdapters) {
            adapter = contactAdapters[0];
            Log.d("BackGround", "doInBackground: Loading Contacts!");
            // Android version is lesser than 6.0 or the permission is already granted.
            Cursor phones = getContext().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            while (phones.moveToNext()){
                if (active){
                    String id = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                    Contact contact = new Contact(id, name, phoneNumber);
                    mRef.collection("user").whereEqualTo("phone", contact.getNumber())
                            .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if(queryDocumentSnapshots.getDocuments().size()==1){
                                DocumentSnapshot snapshot = queryDocumentSnapshots.getDocuments().get(0);
                                User u = snapshot.toObject(User.class);
                                contact.setUser(u);
                                contact.setReference(snapshot.getReference());
                                adapter.add(contact);
                            }
                        }
                    });
                } else {
                    Log.d("Loop", "doInBackground: Loop Broken");
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d("AsyncTask", "onPostExecute: Complete");
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d("Cancel", "onCancelled: ");
            active = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        a.cancel(true);
        Log.d("Pay", "onPause: Called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        a.cancel(true);
        Log.d("Pay", "onDestroy: Called");
    }
}
