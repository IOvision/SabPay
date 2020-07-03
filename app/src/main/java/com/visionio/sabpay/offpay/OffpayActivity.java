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
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.hash.HashCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;


public class OffpayActivity extends AppCompatActivity {

    Button btn_pay, btn_advertise, btn_scan;

    EditText amount;
    private ConnectionsClient connectionsClient;
    private String EndpointId;
    private boolean ongoing = false;
    RippleBackground rippleBackground;
    ImageView imageView;
    TextInputLayout til;
    User user;
    TextView username, balance;


    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    OffPayTransaction a = new OffPayTransaction(payload.asBytes());
                    user = getUser();
                    user.receive(a.getAmount());
                    balance.setText("Balance :" + user.getOffPayBalance());
                    Paper.book("user").write("user",user);
                    /*if (Paper.book().contains("transactions")){
                        ArrayList<byte[]> transactions = Paper.book().read("transactions");
                        transactions.add(payload.asBytes());
                    } else {
                        ArrayList<byte[]> transactions = new ArrayList<>();
                        transactions.add(payload.asBytes());
                        Paper.book().write("transactions", transactions);
                    }*/
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
                        payMode();
                        Log.d("Connection", "onConnectionResult: connection successful");
                        connectionsClient.stopDiscovery();
                        EndpointId = endpointId;
                        Toast.makeText(OffpayActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("Connection", "onConnectionResult: connection failed");
                        Toast.makeText(OffpayActivity.this, result.getStatus().getStatusMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d("Connection", "onDisconnected: disconnected from the opponent");
                    scanMode();
                }
            };

    private void payMode() {
        btn_pay.setVisibility(View.VISIBLE);
        til.setVisibility(View.VISIBLE);
        btn_scan.setVisibility(View.GONE);
        btn_advertise.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        rippleBackground.setVisibility(View.GONE);
    }

    private void scanMode() {
        btn_pay.setVisibility(View.GONE);
        til.setVisibility(View.GONE);
        btn_scan.setVisibility(View.VISIBLE);
        btn_advertise.setVisibility(View.VISIBLE);
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i("Connection", "onEndpointFound: endpoint found, connecting");
                    connectionsClient.requestConnection(FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), endpointId, connectionLifecycleCallback);
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

        connectionsClient = Nearby.getConnectionsClient(this);

        amount = findViewById(R.id.offpay_amount_et);
        btn_scan = findViewById(R.id.offpay_scan_btn);
        btn_advertise = findViewById(R.id.offpay_advertise);
        btn_pay = findViewById(R.id.offpay_pay_btn);
        rippleBackground=(RippleBackground)findViewById(R.id.content);
        imageView = findViewById(R.id.centerImage);
        til = findViewById(R.id.offpay_amount);

        username = findViewById(R.id.offpay_username);
        balance = findViewById(R.id.offpay_balance);

        user = getUser();

        username.setText(user.getName());
        balance.setText("Balance :" + user.getOffPayBalance());

        btn_scan.setOnClickListener(v -> {
            if (ongoing) {
                disconnect();
                Toast.makeText(this, "disconnect", Toast.LENGTH_SHORT).show();
            } else {
                advertise();
                Toast.makeText(this, "advertise", Toast.LENGTH_SHORT).show();
            }
        });

        btn_pay.setOnClickListener(v -> {
            pay(Integer.parseInt(amount.getText().toString()));
        });

        btn_advertise.setOnClickListener(v -> {
            discover();
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500);
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(OffpayActivity.this, "OFFPAY_ACTIVITY_SHOWCASE");

        sequence.addSequenceItem(btn_scan, "Sender clicks here ", "Got it");
        sequence.addSequenceItem(btn_advertise, "Receiver clicks here at the same time", "Got it");
        sequence.addSequenceItem(imageView, "nearby device found will be shown to select ", "Got it");
        sequence.start();
        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        ImageView imageView=(ImageView)findViewById(R.id.centerImage);
        imageView.setVisibility(View.VISIBLE);
        rippleBackground.startRippleAnimation();

    }


    private void advertise() {
        startAdvertising();
        btn_scan.setText("Stop");
        ongoing=true;
    }

    private void discover() {
        imageView.setVisibility(View.VISIBLE);
        rippleBackground.startRippleAnimation();
        startDiscovery();
        btn_scan.setText("Stop");
        ongoing=true;
    }

    public void disconnect() {
        EndpointId = null;
        connectionsClient.stopAllEndpoints();
        btn_scan.setText("Scan");
        imageView.setVisibility(View.GONE);
        rippleBackground.stopRippleAnimation();
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


    public User getUser() {
        return Paper.book("user").read("user");
    }

    public void pay(int amount) {
        user = getUser();
        if (user.getOffPayBalance() >= amount){
            OffPayTransaction pay = new OffPayTransaction(FirebaseAuth.getInstance().getUid(), amount);
            Payload payload = Payload.fromBytes(pay.toBytes());
            Nearby.getConnectionsClient(this).sendPayload(EndpointId, payload).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Payload Sent!", Toast.LENGTH_SHORT).show();
            });
            user.send(amount);
            balance.setText(user.getOffPayBalance());
            Paper.book("user").write("user",user);
        } else {
            Toast.makeText(this, "Not Enough Balance!", Toast.LENGTH_SHORT).show();
        }
    }
}
