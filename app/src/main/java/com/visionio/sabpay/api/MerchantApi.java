package com.visionio.sabpay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.visionio.sabpay.interfaces.ApiInterface;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MerchantApi {
    public static final String api_key = "qIEvxBbP8V6e1YLXICde";
    private static final String base_url = "https://asia-east2-sabpay-ab94e.cloudfunctions.net";

    public static ApiInterface apiService = null;

    public static ApiInterface getApiService() {
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
            apiService = retrofit.create(ApiInterface.class);
        }
        return apiService;
    }
}
