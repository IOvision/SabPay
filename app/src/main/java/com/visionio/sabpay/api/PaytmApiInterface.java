package com.visionio.sabpay.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PaytmApiInterface {

    @POST("initiateTransaction")
    Call<Map<String, Object>> getTransactionID(@Query("mid") String mid,
                                               @Query("orderId") String orderId,
                                               @Body Map<String, Object> body);

}
