package com.visionio.sabpay.interfaces;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.visionio.sabpay.adapter.TransactionAdapter;

public interface MainInterface {
    void setBalanceTv(TextView tv, ProgressBar balance_pb, ImageView addMoney);
    void loadTransactions(TransactionAdapter adapter, ProgressBar progressBar);
}
