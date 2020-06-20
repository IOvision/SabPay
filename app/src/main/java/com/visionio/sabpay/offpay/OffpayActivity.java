package com.visionio.sabpay.offpay;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.firebase.auth.FirebaseAuth;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.R;

import io.paperdb.Paper;


public class OffpayActivity extends AppCompatActivity {

    Button btn_receive, btn_pay, btn_find;
    EditText amount;
    private ConnectionsClient connectionsClient;
    private String opponentEndpointId;
    private boolean ongoing = false;
    User user;
    TextView textView;

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Toast.makeText(OffpayActivity.this, "Recieved!" + fromByteArray(payload.asBytes()), Toast.LENGTH_SHORT).show();
                    user.receive(fromByteArray(payload.asBytes()));
                    textView.setText(user.getFirstName() + "," + user.getOffPayBalance());
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                    }
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d("Connection", "onConnectionInitiated: accepting connection");
                    Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, payloadCallback);
                    connectionsClient.stopAdvertising();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        btn_receive.setVisibility(View.VISIBLE);
                        amount.setVisibility(View.VISIBLE);
                        Log.d("Connection", "onConnectionResult: connection successful");
                        connectionsClient.stopDiscovery();
                        opponentEndpointId = endpointId;
                        Toast.makeText(OffpayActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("Connection", "onConnectionResult: connection failed");
                        Toast.makeText(OffpayActivity.this, "Disconnected!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d("Connection", "onDisconnected: disconnected from the opponent");
                    btn_receive.setVisibility(View.GONE);
                    amount.setVisibility(View.GONE);
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i("Connection", "onEndpointFound: endpoint found, connecting");
                    connectionsClient.requestConnection(user.getName(), endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {}
            };

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offpay);

        btn_pay = findViewById(R.id.offpay_btn_pay);
        btn_receive = findViewById(R.id.offpay_btn_receive);
        btn_find = findViewById(R.id.offpay_btn_create);

        amount = findViewById(R.id.offpay_amount);

        textView = findViewById(R.id.offpay_user_name);

        user = getUser();
        if (!user.getUid().equals(FirebaseAuth.getInstance().getUid())){
            Toast.makeText(this, "Cannot use OffPay", Toast.LENGTH_SHORT).show();
            finish();
        }

        textView.setText(user.getFirstName() + "," + user.getOffPayBalance());

        connectionsClient = Nearby.getConnectionsClient(this);

        btn_pay.setOnClickListener(v -> {
            if (ongoing) {
                disconnect();
            } else {
                advertise();
            }
        });

        btn_receive.setOnClickListener(v -> {
            pay(Integer.parseInt(amount.getText().toString()));
        });

        btn_find.setOnClickListener(v -> {
            discover();
        });

    }

    private void advertise() {
        startAdvertising();
        btn_pay.setText("Stop");
        ongoing=true;
    }

    private void discover() {
        startDiscovery();
        btn_pay.setText("Stop");
        ongoing=true;
    }

    public void disconnect() {
        opponentEndpointId = null;
        connectionsClient.stopAllEndpoints();
        btn_pay.setText("Connect");
        ongoing=false;
    }

    private void startDiscovery() {
        connectionsClient.startDiscovery(
                getPackageName(), endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build());
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(
                user.getName(), getPackageName(), connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    static byte[] toBytes(int i){
        byte[] result = new byte[4];
        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);
        return result;
    }

    static int fromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    public User getUser() {
        return Paper.book("user").read("user");
    }

    public void pay(int amount) {
        if(amount <= user.getOffPayBalance()){
            Payload payload = Payload.fromBytes(toBytes(amount));
            Nearby.getConnectionsClient(this).sendPayload(opponentEndpointId, payload).addOnSuccessListener(aVoid -> {
                user.send(amount);
                Paper.book("pending").write("user", user);
                textView.setText(user.getFirstName() + "," + user.getOffPayBalance());
                if(user.getOffPayBalance() == null) {
                    user.setOffPayBalance(200);
                    Paper.book("pending").write("user",user);
                    Paper.book(FirebaseAuth.getInstance().getUid()).write("user",user);
                }
            });
        } else {
            Toast.makeText(this, "Not Enough Balance.", Toast.LENGTH_SHORT).show();
        }
    }
}
