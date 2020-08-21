package com.visionio.sabpay.api;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;
import com.visionio.sabpay.models.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SabPayNotify {
    private String  title;
    private String message;

    public static class Builder{
        SabPayNotify instance = new SabPayNotify();

        public void send(Context context, String to, Boolean toMerchant){
            // to can be 10 digit mobile or uid
            if(instance.title==null || instance.title.equals("")){
                Utils.toast(context, "TITLE IS NULL", Toast.LENGTH_SHORT);
                return;
            }
            if(instance.message==null || instance.title.equals("")){
                Utils.toast(context, "MESSAGE IS NULL", Toast.LENGTH_SHORT);
                return;
            }
            Call<Map<String, Object>> call = API.getApiService().ping(to, instance.title, instance.message, toMerchant?"1":"0");
            call.enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    Map<String, Object> result = response.body();
                    if(!response.isSuccessful() || result==null){
                        String body = null;
                        try {
                            body = response.errorBody().string();
                        } catch (IOException e) {
                            body = "{}";
                        }
                        result = new Gson().fromJson(body, HashMap.class);
                    }
                    Utils.toast(context, result.get("msg").toString(), Toast.LENGTH_LONG);
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Utils.toast(context, t.getLocalizedMessage(), Toast.LENGTH_LONG);
                }
            });
        }

        public Builder setTitle(String title){
            instance.title = title;
            return this;
        }
        public Builder setMessage(String message){
            instance.message = message;
            return this;
        }
    }
}
