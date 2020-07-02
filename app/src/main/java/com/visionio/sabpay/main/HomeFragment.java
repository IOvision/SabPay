package com.visionio.sabpay.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPGService;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;
import com.visionio.sabpay.R;
import com.visionio.sabpay.helpdesk.HelpDeskActivity;
import com.visionio.sabpay.models.AddTransaction;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.models.Wallet;
import com.visionio.sabpay.services.FeedbackActivity;

import java.util.HashMap;
import java.util.Map;

import io.paperdb.Paper;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class HomeFragment extends Fragment {

    LinearLayout linear;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    TextView balanceTv, wallet_text, current_balance_text;
    ExtendedFloatingActionButton addMoney;
    ProgressBar balance_pb, add_money_pg;
    ListenerRegistration listenerRegistration;
    Button btn_add, btn_cancel;
    ExtendedFloatingActionButton helpDesk_btn, feedback_btn;
    RelativeLayout add;
    String amount;
    EditText et_amount;
    Map<String, String> paramMap;
    //AddTransaction transaction = new AddTransaction(amount.getText().toString(), user.getCustomerID());

    public HomeFragment() {
        // Required empty public constructor
    }

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        balanceTv = view.findViewById(R.id.home_balance);
        addMoney = view.findViewById(R.id.home_add_money);
        balance_pb = view.findViewById(R.id.home_balance_pb);
        helpDesk_btn = view.findViewById(R.id.home_help_desk_btn);
        add = view.findViewById(R.id.home_add);
        btn_add = view.findViewById(R.id.home_add_btn);
        btn_cancel = view.findViewById(R.id.home_add_cancel);
        et_amount = view.findViewById(R.id.home_add_et);
        feedback_btn = view.findViewById(R.id.home_feedback_btn);
        add_money_pg = view.findViewById(R.id.home_add_money_pg);
        wallet_text = view.findViewById(R.id.home_your_wallet);
        current_balance_text = view.findViewById(R.id.home_current_balance);
        linear = view.findViewById(R.id.home_linear);
        feedback_btn.setOnClickListener(v -> startActivity(new Intent(getActivity(), FeedbackActivity.class)));

        helpDesk_btn.setOnClickListener(v -> startActivity(new Intent(getActivity(), HelpDeskActivity.class)));

        Sprite wave = new ThreeBounce();
        balance_pb.setIndeterminateDrawable(wave);
        ((MainActivity)getActivity()).setTitle("Hi, " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setBalanceTv();
        addMoney.setOnClickListener(v -> {
            wallet_text.setVisibility(View.INVISIBLE);
            balanceTv.setVisibility(View.INVISIBLE);
            current_balance_text.setVisibility(View.INVISIBLE);
            add.setVisibility(View.VISIBLE);
        });
        btn_add.setOnClickListener(v -> {
            add_money_pg.setVisibility(View.VISIBLE);
            amount = et_amount.getText().toString();
            initializeTransaction();
        });
        btn_cancel.setOnClickListener(v -> {
            wallet_text.setVisibility(View.VISIBLE);
            balanceTv.setVisibility(View.VISIBLE);
            current_balance_text.setVisibility(View.VISIBLE);
            add.setVisibility(View.GONE);
        });
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    @Override
    public void onResume() {
        super.onResume();

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500);
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), "HOME_FRAGMENT_SHOWCASE");

        sequence.addSequenceItem(balanceTv, "Check your wallet balance here", "Got it");
        sequence.addSequenceItem(addMoney, "Add money to your SabPay wallet", "Got it");
        sequence.addSequenceItem(helpDesk_btn, "Post any queries or any help needed", "Got it");
        sequence.addSequenceItem(feedback_btn, "Don't forget to provide us with your feedback", "Got it");
        sequence.addSequenceItem(linear, "When offline use your amazing new feature 'Offpay'", "Got it");
        sequence.start();
    }

    public void setBalanceTv() {
        listenerRegistration = mRef.collection("user").document(mAuth.getUid())
                .collection("wallet").document("wallet")
                .addSnapshotListener((documentSnapshot, e) -> {
                    {
                        Wallet wallet = documentSnapshot.toObject(Wallet.class);
                        balanceTv.setText("\u20B9" + wallet.getBalance().toString());
                        balance_pb.setVisibility(View.GONE);
                        addMoney.setVisibility(View.VISIBLE);
                    }
                });
        Utils.registrations.add(listenerRegistration);
    }

    private void initializeTransaction() {

        DocumentReference DocRef = FirebaseFirestore.getInstance().collection("user").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
        DocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {

                    User user = task.getResult().toObject(User.class);
                    AddTransaction transaction = new AddTransaction(amount, user.getUid());

                    //getting orderID from database
                    DocumentReference documentReference = FirebaseFirestore.getInstance().collection("public").document("Paytm");
                    documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot documentSnapshot = task.getResult();
                                if (documentSnapshot.exists()) {
                                    String test = documentSnapshot.getString("orderId");
                                    transaction.setOrderID(test);
                                    transaction.setUrl("https://us-central1-payment-1de29.cloudfunctions.net/generateChecksum?oID=" + transaction.getOrderID() + "&custID=" + transaction.getCustomerID() + "&amount=" + transaction.getAmount());

                                    //updated orderId
                                    DocumentReference order  = FirebaseFirestore.getInstance().collection("public").document("Paytm");
                                    order.update("orderId", Integer.toString(Integer.parseInt(transaction.getOrderID())+1));
                                    getChecksum(transaction);

                                } else {
                                    Log.d("Document Snapshot", "No such document");
                                }
                            } else {
                            }
                        }
                    });

                }
            }
        });
    }

    private void getChecksum(AddTransaction transaction) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, transaction.getUrl(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Response",response);
                transaction.setChecksum(response);
                initiateTransaction(transaction);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("GET request", "Damn! That didn't work");
            }
        });

        queue.add(stringRequest);
    }

    private void initiateTransaction(AddTransaction transaction) {
        String CallbackURL = "https://securegw-stage.paytm.in/theia/paytmCallback?ORDER_ID=".concat(transaction.getOrderID());
        paramMap = new HashMap<String, String>();
        paramMap.put("CALLBACK_URL", CallbackURL);
        paramMap.put("CHANNEL_ID", "WAP");
        paramMap.put("CUST_ID", transaction.getCustomerID());
        paramMap.put("INDUSTRY_TYPE_ID", "Retail");
        paramMap.put("MID", "SNjnoG01015198317056");
        paramMap.put("WEBSITE", "WEBSTAGING");
        paramMap.put("ORDER_ID", transaction.getOrderID());
        paramMap.put("TXN_AMOUNT", transaction.getAmount());
        paramMap.put("CHECKSUMHASH", transaction.getChecksum().trim());
        Log.d("Checksum", "@"+transaction.getChecksum());
        PaytmOrder order = new PaytmOrder(paramMap);
        PaytmPGService pgService = PaytmPGService.getStagingService();
        pgService.initialize(order, null);
        pay(pgService);
    }

    private void pay(PaytmPGService pgService) {
        pgService.startPaymentTransaction(getContext(), true, true,
                new PaytmPaymentTransactionCallback() {

                    //on successful payment
                    @Override
                    public void onTransactionResponse(Bundle inResponse) {
                        Log.d("paytm", "entered");
                        Log.d("paytmtransaction result", "Payment successful: " + inResponse.toString());
                        updateWallet();

                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final AlertDialog show = alert.show();
                        alert.setTitle("Successful!!!");
                        alert.setMessage("Wallet balance updated");
                        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                show.dismiss();
                            }
                        });
                        alert.show();

                    }

                    @Override
                    public void networkNotAvailable() {
                        Log.d("paytmtransaction result", "Network unavailable");
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final AlertDialog show = alert.show();
                        alert.setTitle("Unsuccessful!!!");
                        alert.setMessage("Network error occurred. Check your network connectivity and try again later.");
                        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                show.dismiss();
                            }
                        });
                        alert.show();
                    }

                    @Override
                    public void clientAuthenticationFailed(String inErrorMessage) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final AlertDialog show = alert.show();
                        alert.setTitle("Unsuccessful!!!");
                        alert.setMessage("Error occurred. Please try again later.");
                        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                show.dismiss();
                            }
                        });
                        alert.show();
                    }

                    @Override
                    public void someUIErrorOccurred(String inErrorMessage) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final AlertDialog show = alert.show();
                        alert.setTitle("Unsuccessful!!!");
                        alert.setMessage("Error occurred. Please try again later.");
                        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                show.dismiss();
                            }
                        });
                        alert.show();
                    }

                    @Override
                    public void onErrorLoadingWebPage(int iniErrorCode, String inErrorMessage, String inFailingUrl) {
                        Log.d("paytmtransaction result", "error loading page response " + inErrorMessage + " + " + inFailingUrl);
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final AlertDialog show = alert.show();
                        alert.setTitle("Unsuccessful!!!");
                        alert.setMessage("Error loading webpage. Please try again later.");
                        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                show.dismiss();
                            }
                        });
                        alert.show();
                    }

                    @Override
                    public void onBackPressedCancelTransaction() {
                        Log.d("paytmtransaction result", "cancel call back response");
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final AlertDialog show = alert.show();
                        alert.setTitle("Unsuccessful!!!");
                        alert.setMessage("Oops transaction was cancelled");
                        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                show.dismiss();
                            }
                        });
                        alert.show();
                    }

                    @Override
                    public void onTransactionCancel(String inErrorMessage, Bundle inResponse) {
                        Log.d("paytmtransaction result", "Transaction cancel");
                        Toast.makeText(getContext(), "You canceled the transaction :(", Toast.LENGTH_SHORT).show();
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final AlertDialog show = alert.show();
                        alert.setTitle("Unsuccessful!!!");
                        alert.setMessage("Transaction was canceled");
                        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                show.dismiss();
                            }
                        });
                        alert.show();

                    }
                });
    }

    private void updateWallet() {
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();
        final DocumentReference documentReference;

        documentReference = mRef.collection("user").document(mAuth.getUid());
        documentReference.collection("wallet").document("wallet").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    Wallet wallet = task.getResult().toObject(Wallet.class);
                    Integer initialWalletAmount = wallet.getBalance();
                    Integer totalAmount = initialWalletAmount + Integer.parseInt(amount);
                    wallet.setBalance(totalAmount);

                    DocumentReference walletBalance  = documentReference.collection("wallet").document("wallet");
                    walletBalance.update("balance", totalAmount);
                    Toast.makeText(getContext(), "Check your wallet balance for a surprise", Toast.LENGTH_SHORT).show();
                    FirebaseFirestore.getInstance().collection("money_add").document(paramMap.get("ORDER_ID")).set(paramMap);
                } else {
                    Log.d("fetching data", "Error fetching wallet data");

                }
            }
        });
    }
}