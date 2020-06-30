package com.visionio.sabpay.offpay;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.hash.HashCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.skyfishjy.library.RippleBackground;
import com.visionio.sabpay.models.OffPayTransaction;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.paperdb.Paper;


public class OffpayActivity extends AppCompatActivity {

    //Button btn_pay, btn_scan, btn_find;
    EditText amount;
    private ConnectionsClient connectionsClient;
    private String EndpointId;
    private boolean ongoing = false;
    TextView textView;

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Toast.makeText(OffpayActivity.this, "Recieved!" + fromByteArray(payload.asBytes()), Toast.LENGTH_SHORT).show();
                    String string = new String(payload.asBytes());
                    if (Paper.book().contains("transactions")){
                        ArrayList<byte[]> transactions = Paper.book().read("transactions");
                        transactions.add(payload.asBytes());
                    } else {
                        ArrayList<byte[]> transactions = new ArrayList<>();
                        transactions.add(payload.asBytes());
                        Paper.book().write("transactions", transactions);
                    }
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
                    MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(getApplicationContext());
                    alert.setTitle("User Found")
                            .setMessage("Connect to " + connectionInfo.getEndpointName() + "?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, payloadCallback);
                                connectionsClient.stopAdvertising();
                            })
                            .setNegativeButton("No", ((dialog, which) -> {
                                dialog.dismiss();
                            }));
                    alert.show();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        //btn_pay.setVisibility(View.VISIBLE);
                        amount.setVisibility(View.VISIBLE);
                        Log.d("Connection", "onConnectionResult: connection successful");
                        connectionsClient.stopDiscovery();
                        EndpointId = endpointId;
                        Toast.makeText(OffpayActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("Connection", "onConnectionResult: connection failed");
                        Toast.makeText(OffpayActivity.this, "Disconnected!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d("Connection", "onDisconnected: disconnected from the opponent");
                    //btn_pay.setVisibility(View.GONE);
                    amount.setVisibility(View.GONE);
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i("Connection", "onEndpointFound: endpoint found, connecting");
                    connectionsClient.requestConnection("A", endpointId, connectionLifecycleCallback);
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


        //textView.setText(user.getFirstName() + "," + user.getOffPayBalance());

        connectionsClient = Nearby.getConnectionsClient(this);

        /*btn_scan.setOnClickListener(v -> {
            if (ongoing) {
                disconnect();
            } else {
                advertise();
            }
        });

        //btn_pay.setOnClickListener(v -> {
            pay(Integer.parseInt(amount.getText().toString()));
        });

        btn_find.setOnClickListener(v -> {
            discover();
        });*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        ImageView imageView=(ImageView)findViewById(R.id.centerImage);
        imageView.setVisibility(View.VISIBLE);
        rippleBackground.startRippleAnimation();
    }

    private void advertise() {
        startAdvertising();
        //btn_scan.setText("Stop");
        ongoing=true;
    }

    private void discover() {
        startDiscovery();
        //btn_scan.setText("Stop");
        ongoing=true;
    }

    public void disconnect() {
        EndpointId = null;
        connectionsClient.stopAllEndpoints();
        //0btn_scan.setText("Connect");
        ongoing=false;
    }

    private void startDiscovery() {
        connectionsClient.startDiscovery(
                getPackageName(), endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build());
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), getPackageName(), connectionLifecycleCallback,
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

    static int fromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    public User getUser() {
        return Paper.book("user").read("user");
    }

    public void pay(int amount) {
        OffPayTransaction pay = new OffPayTransaction("abc", amount);
        Toast.makeText(this, "Doing Something!", Toast.LENGTH_SHORT).show();
        Payload payload = Payload.fromBytes(pay.toBytes());
        Nearby.getConnectionsClient(this).sendPayload(EndpointId, payload).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Payload Sent!", Toast.LENGTH_SHORT).show();
        });
    }
}
