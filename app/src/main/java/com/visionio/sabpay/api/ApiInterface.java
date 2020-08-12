package com.visionio.sabpay.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {

    @GET("transaction_api")
    Call<Map<String, Object>> pay(@Query("from") String senderUid, @Query("to") String receiverMobNo,
                                  @Query("amount") double amount, @Query("api_key") String api_key);

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
