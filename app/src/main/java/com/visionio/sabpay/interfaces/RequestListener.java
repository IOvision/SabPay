package com.visionio.sabpay.interfaces;

import com.visionio.sabpay.enums.REQUEST;

public interface RequestListener {
    void onRequestReceived(REQUEST request);
}

