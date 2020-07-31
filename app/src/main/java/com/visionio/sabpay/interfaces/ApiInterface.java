package com.visionio.sabpay.interfaces;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {

    @POST("test")
    Call<Map<String, Object>> test(@Body Map<String, Object> body);


    @POST("placeOrder")
    Call<Map<String, Object>> placeOrder(@Query ("id") String userId,
                                         @Query("api_key") String api_key,
                                         @Body Map<String, Object> body);

    @POST("generateInvoice")
    Call<Map<String, Object>> generateInvoice(@Query("api_key") String api_key, @Body Map<String, Object> body);


    @GET("refund")
    Call<Map<String, Object>> refundTransaction(@Query("userId") String userId,
                                                @Query("transactionId") String transactionId,
                                                @Query("api_key") String api_key);
}
