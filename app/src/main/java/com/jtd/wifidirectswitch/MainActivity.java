package com.jtd.wifidirectswitch;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity {

    public static final String UPDATE_UI = "com.jtd.intent.UPDATE_UI";
    public static final String ENABLED = "enabled";
    public static final String SSID = "ssid";
    public static final String PASSWORD = "password";
    private final MainActivity ref = this;
    TextView textView_status = null;
    TextView textView_ssid = null;
    TextView textView_password = null;

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
            } else {
                textView_status.setText("Status: Disabled");
                textView_ssid.setText("SSID: (none)");
                textView_password.setText("Password: (none)");
            }
        }
    }

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UIReceiver mReceiver = new UIReceiver();
        IntentFilter filter= new IntentFilter(UPDATE_UI);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        final Button button_enable = (Button) findViewById(R.id.button_enable);
        button_enable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, WifiDirectOperatorService.class);
                i.setAction(WifiDirectOperatorService.ACTION_ENGAGE);
                startService(i);
            }
        });

        final Button button_disable = (Button) findViewById(R.id.button_disable);
        button_disable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, WifiDirectOperatorService.class);
                i.setAction(WifiDirectOperatorService.ACTION_DISENGAGE);
                startService(i);
            }
        });

        textView_status = (TextView) findViewById(R.id.textView_status);
        textView_ssid = (TextView) findViewById(R.id.textView_ssid);
        textView_password = (TextView) findViewById(R.id.textView_password);

        WifiP2pManager mManager = null;
        WifiP2pManager.Channel mChannel = null;
        mManager = (WifiP2pManager) this.getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {

            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                if (wifiP2pGroup == null) {
                    return;
                }
                Log.i("MainActivity", "Found it!");
                Intent i = new Intent(MainActivity.UPDATE_UI);
                i.putExtra(MainActivity.ENABLED, true);
                i.putExtra(MainActivity.SSID, wifiP2pGroup.getNetworkName());
                i.putExtra(MainActivity.PASSWORD, wifiP2pGroup.getPassphrase());
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(ref);
                localBroadcastManager.sendBroadcast(i);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
