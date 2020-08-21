package com.visionio.sabpay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.visionio.sabpay.BuildConfig;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class API {
    public static String _debug_url_ = "https://asia-east2-sabpay-test.cloudfunctions.net";
    private static String _release_url_ = "https://asia-east2-sabpay-ab94e.cloudfunctions.net";
    public static final String api_key = "qIEvxBbP8V6e1YLXICde";
    private static  String base_url = _release_url_;

    public static ApiInterface apiService = null;

    public static ApiInterface getApiService() {
        if(BuildConfig.DEBUG){
            base_url = _debug_url_;
        }
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
