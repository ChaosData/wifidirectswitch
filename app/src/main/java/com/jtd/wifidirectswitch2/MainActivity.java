package com.jtd.wifidirectswitch2;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    public static final String UPDATE_UI = "com.jtd.intent.UPDATE_UI";
    public static final String ENABLED = "enabled";
    public static final String SSID = "ssid";
    public static final String PASSWORD = "password";
    private final MainActivity ref = this;
    TextView textView_status = null;
    TextView textView_ssid = null;
    TextView textView_password = null;
    TextView textView_ip = null;
    TextView textView_errors = null;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;

    private ActivityResultLauncher<String[]> requestPermissionLauncher =
        registerForActivityResult(new RequestMultiplePermissions(), granted_map -> {
            StringBuffer errors = new StringBuffer("");
            granted_map.forEach((k,v) -> {
//                Log.e("NCC", k + ": " + v);
                if (!v) {
                    errors.append("Missing ").append(k).append("\n");
                }
            });
            textView_errors.setText(errors);
        });

    public static String getLocalIPAddress() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.getName().matches(".*p2p-p2p0.*")) {
                    for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                        if (addr instanceof Inet4Address) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
        } catch (NullPointerException ex) {
        }
        return null;
    }

    private class UIReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean enabled = intent.getBooleanExtra(ENABLED, false);
            if (enabled) {
                String ssid = intent.getStringExtra(SSID);
                if (ssid == null) {
                    return;
                }
                String password = intent.getStringExtra(PASSWORD);
                if (password == null) {
                    return;
                }

                textView_status.setText("Status: Enabled");
                textView_ssid.setText("SSID: " + ssid);
                textView_password.setText("Password: " + password);

                String ip = getLocalIPAddress();
                if (ip != null) {
                    textView_ip.setText("IP: " + ip);
                } else {
                    textView_ip.setText("IP: (unknown)");
                }

            } else {
                textView_status.setText("Status: Disabled");
                textView_ssid.setText("SSID: (none)");
                textView_password.setText("Password: (none)");
                textView_ip.setText("IP: (none)");
            }
        }
    }

    private boolean permissionCheck(String[] permissions) {
//        Log.e("NCC", "checking permissions of: " + Arrays.toString(permissions));
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView_errors = (TextView) findViewById(R.id.errors);
        textView_errors.setTextIsSelectable(true);

        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            textView_errors.setText(sw.toString());
            return;
        }

        String[] permissions = info.requestedPermissions;//This array contains the requested permissions.

        if (!permissionCheck(permissions)) {
            requestPermissionLauncher.launch(permissions);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        UIReceiver mReceiver = new UIReceiver();
        IntentFilter filter = new IntentFilter(UPDATE_UI);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        final Button button_enable = (Button) findViewById(R.id.button_enable);
        button_enable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, WifiDirectOperatorService.class);
                i.setAction(WifiDirectOperatorService.ACTION_ENGAGE);
//                startService(i);
                WifiDirectOperatorService.enqueueWork(MainActivity.this, i);
            }
        });

        final Button button_disable = (Button) findViewById(R.id.button_disable);
        button_disable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, WifiDirectOperatorService.class);
                i.setAction(WifiDirectOperatorService.ACTION_DISENGAGE);
//                startService(i);
                WifiDirectOperatorService.enqueueWork(MainActivity.this, i);

            }
        });

        textView_status = (TextView) findViewById(R.id.textView_status);
        textView_ssid = (TextView) findViewById(R.id.textView_ssid);
        textView_password = (TextView) findViewById(R.id.textView_password);
        textView_ip = (TextView) findViewById(R.id.textView_ip);

        WifiP2pManager mManager = null;
        WifiP2pManager.Channel mChannel = null;
        mManager = (WifiP2pManager) this.getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {

            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                if (wifiP2pGroup == null) {
//                    textView_errors.setText("wifiP2pGroup == null");
                    return;
                }
//                textView_errors.setText("");
//                Log.e("NCC", "Found it!");
                Intent i = new Intent(MainActivity.UPDATE_UI);
                i.putExtra(MainActivity.ENABLED, true);
                i.putExtra(MainActivity.SSID, wifiP2pGroup.getNetworkName());
                i.putExtra(MainActivity.PASSWORD, wifiP2pGroup.getPassphrase());

                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(ref);
                localBroadcastManager.sendBroadcast(i);
            }
        });

    }
}