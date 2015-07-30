/**
* Copyright 2015 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.mobilefirst.androidnativebeacons;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.altbeacon.beacon.Beacon;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.worklight.wlclient.api.WLBeacon;
import com.worklight.wlclient.api.WLBeaconTrigger;
import com.worklight.wlclient.api.WLBeaconTriggerAssociation;
import com.worklight.wlclient.api.WLBeaconsAndTriggersJSONStoreManager;
import com.worklight.wlclient.api.WLBeaconsMonitoringApplication;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLBeaconsMonitoringApplication.WLAlertHandler;
import com.worklight.wlclient.api.WLBeaconsMonitoringApplication.WLRangingStatusHandler;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

	private TextView textView = null;

	private WLBeaconsMonitoringApplication wlBeaconsApplication;

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		textView = (TextView) findViewById(R.id.textView);

		WLClient.createInstance(this);

		wlBeaconsApplication = ((WLBeaconsMonitoringApplication) this.getApplication());
		wlBeaconsApplication.setAlertHandler(new WLAlertHandler(this));
		wlBeaconsApplication.setRangingStatusHandler(new RangingStatusHandler()); // optional
	}

	private WLBeaconsAndTriggersJSONStoreManager getJSONStoreManager() {
		return WLBeaconsAndTriggersJSONStoreManager.getInstance(getApplicationContext());
	}

	public void loadBeaconsAndTriggers(View v) {
		updateTextView("Loading beacons and triggers from server...");
		String adapterName = "BeaconsAdapter";
		String procedureName = "getBeaconsAndTriggers";
		getJSONStoreManager().loadBeaconsAndTriggers(adapterName, procedureName, new WLResponseListener() {
			@Override
			public void onSuccess(WLResponse arg0) {
				showBeaconsAndTriggers(null);
			}

			@Override
			public void onFailure(WLFailResponse response) {
				String responseText = "WLBeaconsAndTriggersJSONStoreManager.loadBeaconsAndTriggers() failed:\n"
						+ response.toString();
				Log.d(TAG, responseText);
				updateTextView(responseText);
			}
		});
	}

	public void showBeaconsAndTriggers(View v) {
		StringBuilder stringBuilder = new StringBuilder("Beacons:\n");
		List<WLBeacon> beacons = getJSONStoreManager().getBeaconsFromJsonStore();
		for (int i = 0; i < beacons.size(); i++) {
			WLBeacon beacon = beacons.get(i);
			stringBuilder.append((i + 1) + ") " + beacon.toString() + "\n\n");
		}
		stringBuilder.append("\nBeaconTriggers:\n");
		List<WLBeaconTrigger> beaconTriggers = getJSONStoreManager().getBeaconTriggersFromJsonStore();
		for (int i = 0; i < beaconTriggers.size(); i++) {
			WLBeaconTrigger beaconTrigger = beaconTriggers.get(i);
			stringBuilder.append((i + 1) + ") " + beaconTrigger.toString() + "\n\n");
		}
		stringBuilder.append("\nBeaconTriggerAssociations:\n");
		List<WLBeaconTriggerAssociation> beaconTriggerAssociations = getJSONStoreManager()
				.getBeaconTriggerAssociationsFromJsonStore();
		for (int i = 0; i < beaconTriggerAssociations.size(); i++) {
			WLBeaconTriggerAssociation beaconTriggerAssociation = beaconTriggerAssociations.get(i);
			stringBuilder.append((i + 1) + ") " + beaconTriggerAssociation.toString() + "\n\n");
		}
		updateTextView(stringBuilder.toString());
	}

	public void startMonitoring(View v) {
		updateTextView("Starting monitoring...");
		try {
			wlBeaconsApplication.startMonitoring();
			updateTextView("Beacon monitoring started.");
		} catch (RemoteException e) {
			updateTextView(e.toString());
		}
	}

	public void stopMonitoring(View v) {
		updateTextView("Stopping monitoring...");
		try {
			wlBeaconsApplication.stopMonitoring();
			// Optionally reset monitoring/ranging state
			wlBeaconsApplication.resetMonitoringRangingState();
			updateTextView("Beacon monitoring stopped.");
		} catch (RemoteException e) {
			updateTextView(e.toString());
		}
	}

	private void updateTextView(final String str) {
		runOnUiThread(new Runnable() {
			public void run() {
				String timeStamp = simpleDateFormat.format(new Date());
				textView.setText(timeStamp + "\n" + str);
			}
		});
	}

	private class RangingStatusHandler implements WLRangingStatusHandler {
		@Override
		public void notifyRangedBeacons(Collection<Beacon> beacons) {
			StringBuilder beaconDetails = new StringBuilder();
			for (Beacon beacon : beacons) {
				beaconDetails.append("Beacon " + beacon.toString() + " is about " + beacon.getDistance()
						+ " meters away, with Rssi: " + beacon.getRssi() + "\n");
			}
			updateTextView(beaconDetails.toString());
		}
	}
}
