package com.visionio.sabpay.payment;

import android.animation.Animator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.visionio.sabpay.MainActivity;
import com.visionio.sabpay.R;

public class PaymentHandler {


    Context context;
    Activity activity;
    View.OnClickListener clickListener;

    Dialog dialog;

    //view in dialog layout
    TextView status;
    TextView transactionId;
    TextView date;
    TextView balance;

    TextView linkedWallet;

    EditText amount;

    Button pay;
    Button cancel;

    FrameLayout overlay;

    ImageView statusImageView;
    ProgressBar progressBar;


    public PaymentHandler(Context context, Activity activity, View.OnClickListener clickListener) {
        this.context = context;
        this.activity = activity;
        this.clickListener = clickListener;
        setUpDialog();
    }

    private void setUpDialog(){
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.payment_status_layout);
        dialog.getWindow().setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);

        status = dialog.findViewById(R.id.payment_status_statusText_tv);
        transactionId = dialog.findViewById(R.id.payment_status_transactionId_tv);
        date = dialog.findViewById(R.id.payment_status_date_tv);
        balance = dialog.findViewById(R.id.payment_status_balance_tv);
        statusImageView =  dialog.findViewById(R.id.payment_status_status_iv);
        progressBar = dialog.findViewById(R.id.payment_status_progress_pb);
        overlay = dialog.findViewById(R.id.payment_status_overlay_fl);
        pay = dialog.findViewById(R.id.payment_status_pay_btn);
        cancel = dialog.findViewById(R.id.payment_status_cancel_btn);
        linkedWallet = dialog.findViewById(R.id.payment_status_linkedWalletText_tv);
        amount = dialog.findViewById(R.id.payment_status_amount);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

    }

    public void showPayStatus(){
        pay.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);
        linkedWallet.setVisibility(View.GONE);
        overlay.setVisibility(View.GONE);
        amount.setVisibility(View.GONE);
    }

    public void setLinkedWallet(String name){
        linkedWallet.setText("This wallet is linked to\n"+name);
        pay.setOnClickListener(clickListener);
    }

    public void updateStatus(String msg){
        status.setText(msg);
    }

    public void setError(String msg){
        updateStatus(msg);
        statusImageView.setImageResource(R.drawable.ic_alert);

        progressBar.animate().scaleX(0).scaleY(0).setDuration(2000).start();
        statusImageView.animate().scaleX(1).scaleY(1).setDuration(2000).start();
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, 2500);
    }

    public void setSuccess(String msg){
        updateStatus(msg);
        statusImageView.setImageResource(R.drawable.ic_confirm);

        progressBar.animate().scaleX(0).scaleY(0).setDuration(2000).start();
        statusImageView.animate().scaleX(1).scaleY(1).setDuration(2000).start();

        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
                Intent i1 = new Intent (context, MainActivity.class);
                context.startActivity(i1);
                activity.finish();
            }
        }, 5000);

    }

    public void setBalance(Integer bal){
        balance.setText(bal.toString());
    }

    public void setTransactionId(String id){
        transactionId.setText(id);
    }

    public void setDate(String dt){
        date.setText(dt);
    }

    public void init(){ dialog.show(); }

    public void dismiss(){ dialog.dismiss(); }

    public int getAmount() {
        if(!amount.getText().toString().isEmpty()) {
            return Integer.parseInt(amount.getText().toString());
        }else{
            amount.setError("Amount cannot be empty");
        }
        return -1;
    }

}

