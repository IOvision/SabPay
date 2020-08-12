package com.visionio.sabpay.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPGService;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;
import com.visionio.sabpay.InvoiceActivity;
import com.visionio.sabpay.R;
import com.visionio.sabpay.helpdesk.HelpDeskActivity;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.models.Wallet;
import com.visionio.sabpay.services.FeedbackActivity;

import java.util.HashMap;
import java.util.Map;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class HomeFragment extends Fragment {

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
    LinearLayout ll;
    //AddTransaction transaction = new AddTransaction(amount.getText().toString(), user.getCustomerID());

    public HomeFragment() {
        // Required empty public constructor
    }

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        ll = view.findViewById(R.id.home_ll);
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
        sequence.addSequenceItem(feedback_btn, "Don't forget to provide us with your valuable feedback", "Got it");
        sequence.addSequenceItem(ll, "Don't forget to use our offpay feature with internet off. To try it switch off your internet and open pay tab. A dialog will redirect you to offpay.", "Got it.");
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
    private Task<String> initializeTransaction() {
        // Create the arguments to the callable function.
        Map<String, String> data = new HashMap<>();
        data.put("amount", amount);

        return FirebaseFunctions.getInstance()
                .getHttpsCallable("getChecksum")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        paramMap = (HashMap<String, String>) task.getResult().getData();
                        PaytmOrder order = new PaytmOrder(paramMap);
                        PaytmPGService pgService = PaytmPGService.getStagingService();
                        pgService.initialize(order, null);
                        pay(pgService);
                        return "done";
                    }
                });
    }
    private void pay(PaytmPGService pgService) {
        pgService.startPaymentTransaction(getContext(), true, true,
                new PaytmPaymentTransactionCallback() {

                    //on successful payment
                    @Override
                    public void onTransactionResponse(Bundle inResponse) {
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
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final AlertDialog show = alert.show();
                        alert.setTitle("Unsuccessful!!!");
                        alert.setMessage("Network error occured. Check your network connectivity and try again later.");
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
                }
            }
        });
    }
}