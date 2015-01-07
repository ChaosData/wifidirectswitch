package com.jtd.wifidirectswitch;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class WifiDirectOperatorService extends IntentService {
    public static final String ACTION_ENGAGE = "com.jtd.intents.ENGAGE";
    public static final String ACTION_DISENGAGE = "com.jtd.intents.DISENGAGE";

    WifiP2pManager mManager = null;
    WifiP2pManager.Channel mChannel = null;
    boolean enabled = false;
    private final WifiDirectOperatorService ref = this;

    public WifiDirectOperatorService() {
        super("WifiDirectOperatorService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (mManager == null) {
            mManager = (WifiP2pManager) this.getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(this, getMainLooper(), null);
        }
        if (intent == null) {
            return;
        }
        switch (intent.getAction()) {
            case ACTION_ENGAGE:
                engage();
                break;
            case ACTION_DISENGAGE:
                disengage();
                break;
            default:
                break;
        }

    }


    private void engage() {
        if (enabled) {
            return;
        }

        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {

                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {

                        if (wifiP2pGroup == null) {
                            return;
                        }
                        enabled = true;
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
            public void onFailure(int i) {
                Log.e("WifiDirectOperatorService", "Failed to create group: " + i);
            }
        });

    }

    private void disengage() {
        mManager.removeGroup(mChannel, null);
        enabled = false;
        Intent i = new Intent(MainActivity.UPDATE_UI);
        i.putExtra(MainActivity.ENABLED, false);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(ref);
        localBroadcastManager.sendBroadcast(i);
    }


}
