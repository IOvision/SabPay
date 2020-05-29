package com.visionio.sabpay.interfaces;

import android.widget.ProgressBar;
import android.widget.TextView;
import com.visionio.sabpay.adapter.TransactionAdapter;

public interface MainInterface {
    void setBalanceTv(TextView tv);
    void loadTransactions(TransactionAdapter adapter, ProgressBar progressBar);
}
