package com.visionio.sabpay.models;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

public class TransactionToken extends AsyncTask<Map<String, Object>, String, String> {

    @Override
    protected String doInBackground(Map<String, Object>... maps) {

        Map<String, Object> data = maps[0];
        try {
            JSONObject paytmParams = new JSONObject();

            JSONObject body = new JSONObject();
            body.put("requestType", "Payment");
            body.put("mid", data.get("mid"));
            body.put("websiteName", "WEBSTAGING");
            body.put("orderId", data.get("orderId"));
            body.put("callbackUrl", data.get("callback"));

            JSONObject txnAmount = new JSONObject();
            txnAmount.put("value", data.get("txnAmount"));
            txnAmount.put("currency", "INR");

            JSONObject userInfo = new JSONObject();
            userInfo.put("custId", data.get("userId"));

            body.put("txnAmount", txnAmount);
            body.put("userInfo", userInfo);

            JSONObject head = new JSONObject();
            head.put("signature", data.get("signature"));

            paytmParams.put("body", body);
            paytmParams.put("head", head);

            String post_data = paytmParams.toString();

            /* for Staging */
            String urlString = "https://securegw-stage.paytm.in/theia/api/v1/initiateTransaction?mid="+data.get("mid")+"&orderId="+data.get("orderId");
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            DataOutputStream requestWriter = new DataOutputStream(connection.getOutputStream());
            requestWriter.writeBytes(post_data);
            requestWriter.close();
            String responseData = "";
            InputStream is = connection.getInputStream();
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(is));
            if ((responseData = responseReader.readLine()) != null) {
                responseData += responseData;
            }
            responseReader.close();
            Log.d("testing", "doInBackground: " + responseData);
            return responseData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

    }
}
