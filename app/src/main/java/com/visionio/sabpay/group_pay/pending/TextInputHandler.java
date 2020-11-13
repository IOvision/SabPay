package com.visionio.sabpay.group_pay.pending;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.visionio.sabpay.models.Transaction;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;

public class TextInputHandler {


    Context context;
    OnItemClickListener<Integer> listener;

    Dialog dialog;

    TextView heading;
    EditText editor;
    RelativeLayout confirm;

    TextView amount_1;
    TextView amount_2;
    TextView amount_3;

    RelativeLayout amount_1_container;
    RelativeLayout amount_2_container;
    RelativeLayout amount_3_container;

    View.OnClickListener confirmListener;

    ImageView tick;
    ProgressBar progress;

    Transaction transaction;

    public TextInputHandler(Context context,OnItemClickListener<Integer> listener) {
        this.context = context;
        this.listener = listener;
        setUp();
    }

    private void setUp(){
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.popup_text_input_layout);
        dialog.getWindow().setLayout(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        heading = dialog.findViewById(R.id.popup_text_input_layout_heading_tv);
        editor = dialog.findViewById(R.id.popup_text_input_layout_enter_et);
        confirm = dialog.findViewById(R.id.popup_text_input_layout_confirmContainer_rl);

        tick = dialog.findViewById(R.id.popup_text_input_layout_tickIcon_iv);
        progress = dialog.findViewById(R.id.popup_text_input_layout_progress_pb);

        amount_1_container = dialog.findViewById(R.id.popup_text_input_layout_suggestion1Container_rl);
        amount_2_container = dialog.findViewById(R.id.popup_text_input_layout_suggestion2Container_rl);
        amount_3_container = dialog.findViewById(R.id.popup_text_input_layout_suggestion3Container_rl);

        amount_1 = dialog.findViewById(R.id.popup_text_input_layout_suggestion1_tv);
        amount_2 = dialog.findViewById(R.id.popup_text_input_layout_suggestion2_tv);
        amount_3 = dialog.findViewById(R.id.popup_text_input_layout_suggestion3_tv);

        setConfirmListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editor.getText().toString().trim().equals("")){
                    editor.setError("Amount can't be empty");
                    return;
                }
                listener.onItemClicked(getAmount(-1), -1, v);
            }
        });

        confirm.setOnClickListener(confirmListener);

        amount_1_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(getAmount(0), 0, v);
            }
        });
        amount_2_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(getAmount(1), 1, v);
            }
        });
        amount_3_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(getAmount(2), 2, v);
            }
        });

    }

    public void setConfirmListener(View.OnClickListener confirmListener) {
        this.confirmListener = confirmListener;
    }

    public void setHeading(String heading){
        this.heading.setText(heading.trim());
    }

    public String getHeading(){
        if(editor.getText().toString().trim().equals("")){
            editor.setError("Can't be empty");
            return null;
        }
        return editor.getText().toString().trim();
    }

    public void setInputType(int type){
        editor.setInputType(type);
    }

    public void setLoading(){
        freeze();
        tick.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    public void setError(String errorMessage){
        editor.setError(errorMessage);
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void setDoneLoading(){
        unFreeze();
        progress.setVisibility(View.GONE);
        tick.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    public void setAmountSuggestion(Integer[] suggestion){
        amount_1.setText(suggestion[0].toString());
        amount_2.setText(suggestion[1].toString());
        amount_3.setText(suggestion[2].toString());
    }

    private Integer getAmount(int pos){
        // pos>> -1 for et, 0 for suggestion1 1 for 2 and 2 for 3
        int res;
        switch (pos){
            case 0:
                res = Integer.parseInt(amount_1.getText().toString().trim());
                break;
            case 1:
                res = Integer.parseInt(amount_2.getText().toString().trim());
                break;
            case 2:
                res = Integer.parseInt(amount_3.getText().toString().trim());
                break;
            default:
                res = Integer.parseInt(editor.getText().toString().trim());
                break;
        }
        return res;
    }

    private void freeze(){
        editor.setEnabled(false);
        setConfirmListener(null);
        amount_1_container.setOnClickListener(null);
        amount_2_container.setOnClickListener(null);
        amount_3_container.setOnClickListener(null);
    }

    private void unFreeze(){
        editor.setEnabled(true);
        setConfirmListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editor.getText().toString().trim().equals("")){
                    editor.setError("Amount can't be empty");
                    return;
                }
                listener.onItemClicked(getAmount(-1), -1, v);
            }
        });
        amount_1_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(getAmount(0), 0, v);
            }
        });
        amount_2_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(getAmount(1), 1, v);
            }
        });
        amount_3_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(getAmount(2), 2, v);
            }
        });
    }

    public void show() {
        dialog.show();
    }

    public void destroy(){
        dialog.dismiss();
    }

}
