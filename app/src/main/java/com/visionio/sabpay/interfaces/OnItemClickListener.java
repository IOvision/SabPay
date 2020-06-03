package com.visionio.sabpay.interfaces;

import android.view.View;

public interface OnItemClickListener<T> {
    void onItemClicked(T object, int position, View view);
}
