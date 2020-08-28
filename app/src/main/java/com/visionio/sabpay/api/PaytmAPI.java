package com.visionio.sabpay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.visionio.sabpay.BuildConfig;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PaytmAPI {
    private static  String base_url = "https://securegw.paytm.in/theia/api/v1/";

    public static PaytmApiInterface apiService = null;

    public static PaytmApiInterface getApiService() {
        if(apiService==null){
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            OkHttpClient client = new OkHttpClient();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(base_url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            apiService = retrofit.create(PaytmApiInterface.class);
        }
        return apiService;
    }
}
