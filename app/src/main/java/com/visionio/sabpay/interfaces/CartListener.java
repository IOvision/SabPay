package com.visionio.sabpay.interfaces;

import com.visionio.sabpay.models.Item;

public interface CartListener {

    void onIncreaseQty(Item item);
    void onDecreaseQty(Item item);
}
