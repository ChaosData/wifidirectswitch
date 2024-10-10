/*
Copyright 2015 iSEC Partners, 2015-2021 NCC Group

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jtd.wifidirectswitch2;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class WifiDirectOperatorService extends JobIntentService {
    public static final String ACTION_ENGAGE = "com.jtd.intents.ENGAGE";
    public static final String ACTION_DISENGAGE = "com.jtd.intents.DISENGAGE";

    static final int JOB_ID = 6666;

    WifiP2pManager mManager = null;
    WifiP2pManager.Channel mChannel = null;
    boolean enabled = false;
    private final WifiDirectOperatorService ref = this;

//    public WifiDirectOperatorService() {
//        super("WifiDirectOperatorService");
//    }

    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WifiDirectOperatorService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                if (ActivityCompat.checkSelfPermission(WifiDirectOperatorService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
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
                String err = "unknown";
                switch (i) {
                    case WifiP2pManager.ERROR:
                        err = "WifiP2pManager.ERROR";
                        break;
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        err = "WifiP2pManager.P2P_UNSUPPORTED";
                        break;
                    case WifiP2pManager.BUSY:
                        err = "WifiP2pManager.BUSY";
                        break;
                }
                String msg = "Failed to create group: " + err + " (" + i + ")";
                Log.e("NCC", msg);
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
