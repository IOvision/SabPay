package com.visionio.sabpay.main;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.Result;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.ContactAdapter;
import com.visionio.sabpay.adapter.SelectedContactsAdapter;
import com.visionio.sabpay.helper.GroupSelectHandler;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.GroupPay;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class PayFragment extends Fragment {

    public PayFragment() {
        // Required empty public constructor
    }

    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    TextInputLayout textInputLayout;
    EditText et_number;

    View.OnClickListener til_listener_show_keyboard;
    View.OnClickListener til_listener_hide_keyboard;

    RecyclerView selectedContactsRecyclerView;
    RecyclerView allContactsRecyclerView;
    ContactAdapter allContactAdapter;
    SelectedContactsAdapter selectedContactsAdapter;
    Boolean selected=false;
    LinearLayout recyclerViewContainer;

    List<Contact> bufferedContacts = new ArrayList<>();

    ImageView overlay;
    ProgressBar progressBar;

    ViewGroup contentFrame;
    ZXingScannerView mScannerView;
    Boolean scannerOpen = true;


    String jsonFromQr;

    String phoneNumber;
    DocumentReference senderDocRef, receiverDocRef;

    Button pay;

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay, container, false);

        textInputLayout = view.findViewById(R.id.pay_fragment_number_textInputLayout);
        et_number = view.findViewById(R.id.pay_number);
        pay = view.findViewById(R.id.pay_btn_pay);
        allContactsRecyclerView = view.findViewById(R.id.pay_fragment_recycler);

        selectedContactsAdapter = new SelectedContactsAdapter(new ArrayList<>());
        allContactAdapter = new ContactAdapter(getContext(), new ArrayList<>(Utils.deviceContacts), new ArrayList<>(Utils.deviceContacts));

        recyclerViewContainer = view.findViewById(R.id.pay_fragment_recyclerViewsContainer_ll);
        selectedContactsRecyclerView = view.findViewById(R.id.pay_fragment_selectedContacts_rv);

        overlay = view.findViewById(R.id.overlay);
        progressBar = view.findViewById(R.id.pay_progress);

        selectedContactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()){{
            setOrientation(RecyclerView.HORIZONTAL);
        }});
        selectedContactsRecyclerView.setHasFixedSize(true);

        allContactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allContactsRecyclerView.setHasFixedSize(false);
        allContactsRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.HORIZONTAL));


        allContactAdapter.setClickListener(new OnItemClickListener<Contact>() {
            @Override
            public void onItemClicked(Contact contact, int position, View v) {
                Log.i("Testing", "Select: "+position);
                for(Contact c: selectedContactsAdapter.getContacts()){
                    if(c.getNumber().equals(contact.getNumber())){
                        Toast.makeText(getContext(), "Already added", Toast.LENGTH_SHORT).show();

                        return;
                    }
                }

                allContactAdapter.select(position);
                selectedContactsAdapter.add(contact);
                if(selectedContactsAdapter.getItemCount()==1 && !selected){
                    selected=true;
                }
            }

        });

        selectedContactsAdapter.setClickListener(new OnItemClickListener<Contact>() {
            @Override
            public void onItemClicked(final Contact contact, final int position, View v) {
                v.animate().scaleX(0).scaleY(0).setInterpolator(new DecelerateInterpolator()).setDuration(500).start();
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //allContactAdapter.addUserToContact(contact);
                        selectedContactsAdapter.remove(contact);
                        if(contact.positionInAdapter != 0){
                            allContactAdapter.unSelect(contact.positionInAdapter);
                        }
                        if(selectedContactsAdapter.getItemCount()==0 && selected){
                            selected = false;
                        }
                    }
                }, 1000);
            }

        });

        selectedContactsRecyclerView.setAdapter(selectedContactsAdapter);
        allContactsRecyclerView.setAdapter(allContactAdapter);

        pay.setOnClickListener(v -> {
            List<Contact> contacts = selectedContactsAdapter.getContacts();
            Toast.makeText(getContext(), "Pay to: "+contacts.size(), Toast.LENGTH_SHORT).show();
        });

        til_listener_show_keyboard = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideScanner();
                et_number.setEnabled(true);
                textInputLayout.setEndIconDrawable(R.drawable.ic_qr_scan);
                textInputLayout.setEndIconOnClickListener(til_listener_hide_keyboard);
            }
        };

        til_listener_hide_keyboard = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScanner();
                hideKeyboard();
                et_number.setEnabled(false);
                textInputLayout.setEndIconDrawable(R.drawable.ic_keyboard_white_24dp);
                textInputLayout.setEndIconOnClickListener(til_listener_show_keyboard);
            }
        };

        textInputLayout.setEndIconDrawable(R.drawable.ic_keyboard_white_24dp);
        textInputLayout.setEndIconOnClickListener(til_listener_show_keyboard);

        et_number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length()==10){
                    searchUser();
                }
                allContactAdapter.applyFilter(s.toString().trim().toLowerCase());
            }
        });

        setUp(view);
        ((MainActivity)getActivity()).setTitle("Pay");
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
                    updateVariableData();
                    searchUser();
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
        updateVariableData();

        boolean buffered = false;

        for(Contact c: bufferedContacts){
            if(phoneNumber.equals(c.getNumber())){
                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(getContext());
                alert
                        .setTitle("Confirm")
                        .setMessage("This wallet is linked to: "+c.getUser().getName())
                        .setNegativeButton("No", ((dialog, which) -> {
                            dialog.dismiss();
                        }))
                        .setPositiveButton("Yes", (dialog, which) -> {
                            selectedContactsAdapter.add(c);
                        }).show();
                buffered = true;
                break;
            }
        }
        if(buffered){
            return;
        }
        mRef.collection("user").whereEqualTo("phone", phoneNumber).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            if(!task.getResult().getDocuments().isEmpty()){
                                DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                                User user = snapshot.toObject(User.class);

                                Contact c = new Contact();
                                c.setName(user.getName());
                                c.setNumber(user.getPhone());
                                c.setUser(user);

                                bufferedContacts.add(c);

                                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(getContext());
                                alert
                                        .setTitle("Confirm")
                                        .setMessage("This wallet is linked to: "+user.getName())
                                        .setNegativeButton("No", ((dialog, which) -> {
                                            dialog.dismiss();
                                        }))
                                        .setPositiveButton("Yes", (dialog, which) -> {
                                            selectedContactsAdapter.add(c);
                                        }).show();

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

    void updateVariableData(){
        phoneNumber = Utils.formatNumber(et_number.getText().toString().trim(), 0);
    }
    private void hideScanner(){
        scannerOpen = false;
        mScannerView.stopCamera();
        contentFrame.setVisibility(View.GONE);
        recyclerViewContainer.setVisibility(View.VISIBLE);
    }

    private void showScanner(){
        scannerOpen = true;
        recyclerViewContainer.setVisibility(View.GONE);
        mScannerView.startCamera();
        contentFrame.setVisibility(View.VISIBLE);
    }



    public void hideKeyboard() {
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
}
