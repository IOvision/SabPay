package com.visionio.sabpay.interfaces;

import android.view.View;

import com.visionio.sabpay.Models.Contact;

public interface OnItemClickListener<T> {
    void onItemClicked(T object, int position, View view);
}
