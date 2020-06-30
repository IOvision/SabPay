package com.visionio.sabpay.main;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.R;
import com.visionio.sabpay.group_pay.manageGroup.GroupManageActivity;
import com.visionio.sabpay.group_pay.manageTransactions.GroupPayAdapter;
import com.visionio.sabpay.group_pay.manageTransactions.ManageTransactionsActivity;
import com.visionio.sabpay.group_pay.manageTransactions.NewGPayHandler;
import com.visionio.sabpay.group_pay.pending.PendingPaymentActivity;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.GroupPay;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class GroupPayFragment extends Fragment {

    Button pending_transactions, my_groups;
    FloatingActionButton  new_gpay;

    RecyclerView recyclerView;
    GroupPayAdapter adapter;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    public GroupPayFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_pay, container, false);

        pending_transactions = view.findViewById(R.id.btn_pending_transactions);
        my_groups = view.findViewById(R.id.btn_my_groups);
        new_gpay = view.findViewById(R.id.btn_new_gpay);
        recyclerView = view.findViewById(R.id.group_pay_recycler_view);
        ((MainActivity)getActivity()).setTitle("Group Pay");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pending_transactions.setOnClickListener(v -> ((MainActivity)getActivity()).startPendingPayment());

        my_groups.setOnClickListener(v -> startActivity(new Intent(getActivity(), GroupManageActivity.class)));

        new_gpay.setOnClickListener(v -> {
            NewGPayHandler handler = new NewGPayHandler(getContext(), mAuth, mRef);
            handler.init();
        });

        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(false);

        adapter = new GroupPayAdapter(getContext(), new ArrayList<GroupPay>(), (OnItemClickListener<GroupPay>) (object, position, view1) -> showQr(object));

        recyclerView.setItemAnimator(new SlideInLeftAnimator());
        recyclerView.setAdapter(adapter);
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();

        super.onResume();

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500);
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), "GROUP_FRAGMENT_SHOWCASE");

        sequence.addSequenceItem(my_groups, "Create your group today!!", "Got it");
        sequence.addSequenceItem(new_gpay, "Pay using gPay", "Got it");
        sequence.addSequenceItem(pending_transactions, "See your pending transactions here", "Got it");
        sequence.start();
    }

    private void showQr(GroupPay groupPay) {
        String qrText = groupPay.getTo().getId()+"__"+groupPay.getId();

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.qr_layout);
        dialog.getWindow().setLayout(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        ImageView imageView = dialog.findViewById(R.id.qr_image_iv);
        imageView.setImageBitmap(Utils.getQrBitmap(qrText));

        dialog.show();
    }

    void loadData(){

        mRef.collection("user/"+mAuth.getUid()+"/group_pay/meta-data/transaction")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        for(DocumentSnapshot snapshot: task.getResult()){
                            GroupPay groupPay = snapshot.toObject(GroupPay.class);
                            adapter.add(groupPay);
                        }
                    }else{
                        Log.i("Testing", task.getException().getLocalizedMessage());
                    }
                });
    }
}