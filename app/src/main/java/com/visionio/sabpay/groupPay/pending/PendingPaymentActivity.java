package com.visionio.sabpay.groupPay.pending;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.Models.GroupPay;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.interfaces.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

public class PendingPaymentActivity extends AppCompatActivity {

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    RecyclerView recyclerView;
    TextView background;

    TransactionAdapter adapter;

    TextInputHandler inputHandler;

    SwipeRefreshLayout refreshLayout;

    GroupPay gPayObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_payment);

        setUp();
    }

    private void setUp(){
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.pending_transaction_items_rv);
        background = findViewById(R.id.pending_transaction_noItemHeading_tv);
        refreshLayout = findViewById(R.id.pending_transaction_refresh_srl);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadPendingTransactions();
            }
        });

        inputHandler = new TextInputHandler(this, new OnItemClickListener<Integer>() {
            @Override
            public void onItemClicked(final Integer object, int position, View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(PendingPaymentActivity.this)
                        .setTitle("Pay?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                initiatePay(object);
                            }
                        })
                        .create();
                alertDialog.show();

            }
        });
        inputHandler.setInputType(InputType.TYPE_CLASS_NUMBER);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        adapter = new TransactionAdapter(new ArrayList<Transaction>(), new OnItemClickListener<Transaction>() {
            @Override
            public void onItemClicked(Transaction object, int position, View view) {
                fetchGroupPayDetails(object);
            }
        }, true);

        recyclerView.setAdapter(adapter);

        loadPendingTransactions();
    }

    private void loadPendingTransactions(){
        mRef.collection("user/"+mAuth.getUid()+"/pending_gPay_transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            if(task.getResult().getDocuments().size()==0 && background.getVisibility()==View.GONE){
                                background.setVisibility(View.VISIBLE);
                                if(adapter!=null){
                                    adapter.allClear();
                                }
                            }
                            for(DocumentSnapshot snapshot: task.getResult()){
                                if(background.getVisibility()==View.VISIBLE){
                                    background.setVisibility(View.GONE);
                                }

                                Transaction transaction = snapshot.toObject(Transaction.class);
                                adapter.add(transaction);
                            }
                            refreshLayout.setRefreshing(false);
                        }else{
                            Log.i("Testing", task.getException().getLocalizedMessage());
                        }
                    }
                });
    }

    private void fetchGroupPayDetails(final Transaction transaction){
        inputHandler.show();
        inputHandler.setLoading();

        transaction.getTo().collection("group_pay/meta-data/transaction/")
                .document(transaction.getgPayId())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    GroupPay groupPay = task.getResult().toObject(GroupPay.class);
                    Integer[] suggestedAmount = new Integer[3];

                    double equal = groupPay.getAmount()/ (double) groupPay.getParts();

                    if(equal == Math.abs(equal)){
                        suggestedAmount[0] = (int) equal;
                        suggestedAmount[1] = (int) equal*2;
                        if(groupPay.getParts()==2){
                            suggestedAmount[2] = (int) (equal + equal/2);
                        }else{
                            suggestedAmount[2] = (int) equal*3;
                        }

                    }else{
                        suggestedAmount[0] = (int) Math.ceil(equal);
                        suggestedAmount[1] = (int) Math.floor(equal);
                        suggestedAmount[2] = suggestedAmount[0]*2;
                    }

                    inputHandler.setDoneLoading();
                    inputHandler.setAmountSuggestion(suggestedAmount);
                    inputHandler.setTransaction(transaction);
                    gPayObject = groupPay;

                }else{
                    Log.i("Testing", task.getException().getLocalizedMessage());
                }
            }
        });
    }

    private void updateData(Integer amount){
        inputHandler.setLoading();
        if(gPayObject.getLedger().size()+1==gPayObject.getParts()){
            int sum = 0;
            List<Integer> ledger = gPayObject.getLedger();
            for(Integer i: ledger){
                sum += i;
            }
            if(sum+amount<gPayObject.getAmount()){
                inputHandler.setError("Amount insufficient");
                inputHandler.setDoneLoading();
                return;
            }
        }
        Transaction transaction = inputHandler.getTransaction();


        transaction.getFrom().collection("pending_gPay_transactions")
                .document(transaction.getId())
                .update("amount", amount).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(PendingPaymentActivity.this, "Done", Toast.LENGTH_SHORT).show();
                    inputHandler.destroy();
                }else{
                    Log.i("Testing", task.getException().getLocalizedMessage());
                }
            }
        });
    }

    void initiatePay(final Integer amount){
        inputHandler.getTransaction().getFrom().collection("wallet").document("wallet").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    Wallet wallet = task.getResult().toObject(Wallet.class);

                    if(amount > wallet.getBalance()){
                        Toast.makeText(PendingPaymentActivity.this, "Insufficient Balance", Toast.LENGTH_LONG).show();
                        inputHandler.setDoneLoading();
                        inputHandler.destroy();
                    }else {
                        updateData(amount);
                    }
                }else{
                    Toast.makeText(PendingPaymentActivity.this, "Insufficient Balance", Toast.LENGTH_LONG).show();
                    inputHandler.setDoneLoading();
                    inputHandler.destroy();
                }
            }
        });
    }

}
