/*
 *
    COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, modify, and distribute
    these sample programs in any form without payment to IBM® for the purposes of developing, using, marketing or distributing
    application programs conforming to the application programming interface for the operating platform for which the sample code is written.
    Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS AND IBM DISCLAIMS ALL WARRANTIES,
    EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY,
    FITNESS FOR A PARTICULAR PURPOSE, TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT,
    INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE SAMPLE SOURCE CODE.
    IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.

 */
package com.worklight.wlclient.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.worklight.wlclient.api.WLBeaconTrigger.WLProximity;
import com.worklight.wlclient.api.WLBeaconTrigger.WLTriggerType;

public class WLBeaconsMonitoringApplication extends Application implements BootstrapNotifier, RangeNotifier {
	private static final String TAG = "WLBeaconsApplication";

	private BeaconManager mBeaconManager;
	private RegionBootstrap mRegionBootstrap;

	private WLRangingStatusHandler rangingStatusHandler = null;
	private WLAlertHandler alertHandler = null;
	private ActivityLifecycleHandler activityLifecycleHandler;

	@Override
	public void onCreate() {
		super.onCreate();

		mBeaconManager = BeaconManager.getInstanceForApplication(this);

		// By default the AndroidBeaconLibrary will only find AltBeacons. If you wish to make it
		// find a different type of beacon, you must specify the byte layout for that beacon's
		// advertisement with a line like below. The example shows how to find a beacon with the
		// same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb. To find the proper
		// layout expression for other beacon types, do a web search for "setBeaconLayout"
		// including the quotes.
		//
		// mBeaconManager.getBeaconParsers().add(new BeaconParser().
		// setBeaconLayout("m:2-3=aabb,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
		
		if (isMonitoringEnabled()) {
			mRegionBootstrap = new RegionBootstrap(this, getUuidRegions());
		}

		@SuppressWarnings("unused")
		BackgroundPowerSaver mBackgroundPowerSaver = new BackgroundPowerSaver(this);
		mBeaconManager.setBackgroundBetweenScanPeriod(1000);

		activityLifecycleHandler = new ActivityLifecycleHandler();
		registerActivityLifecycleCallbacks(activityLifecycleHandler);
	}

	private void verifyBluetooth() {
		String error = null;
		String errorDescription = null;
		try {
			if (!mBeaconManager.checkAvailability()) {
				error = "Bluetooth not enabled";
				errorDescription = "Please enable bluetooth in settings and restart this application.";
			}
		} catch (RuntimeException e) {
			error = "Bluetooth LE not available";
			errorDescription = "Sorry, this device does not support Bluetooth LE.";
		}
		if (error != null) {
			if (alertHandler != null) {
				alertHandler.showAlert(error, errorDescription, true);
			} else {
				Log.e(TAG, error + ": " + errorDescription);
				System.exit(1);
			}
		}
	}

	private WLBeaconsAndTriggersJSONStoreManager getJSONStoreManager() {
		return WLBeaconsAndTriggersJSONStoreManager.getInstance(getApplicationContext());
	}

	private List<Region> getUuidRegions() {
		List<Region> uuidRegions = new ArrayList<Region>();
		for (String uuid : getJSONStoreManager().getUuids()) {
			uuidRegions.add(new Region(uuid, Identifier.parse(uuid), null, null));
		}
		return uuidRegions;
	}

	public void startMonitoring() throws RemoteException {
		verifyBluetooth();
		setMonitoringEnabled(true);
		if (mRegionBootstrap == null) {
			mRegionBootstrap = new RegionBootstrap(this, getUuidRegions());
		}
	}

	public void stopMonitoring() throws RemoteException {
		stopRanging();
		if (mRegionBootstrap != null) {
			mRegionBootstrap.disable();
			mRegionBootstrap = null;
		}
		setMonitoringEnabled(false);
	}

	private void startRanging() throws RemoteException {
		mBeaconManager.setRangeNotifier(this);
		for (Region region : getUuidRegions()) {
			mBeaconManager.startRangingBeaconsInRegion(region);
		}
	}

	private void stopRanging() throws RemoteException {
		for (Region region : getUuidRegions()) {
			mBeaconManager.stopRangingBeaconsInRegion(region);
		}
	}

	@Override
	public void didEnterRegion(Region region) {
		if (isMonitoringEnabled()) {
			try {
				Log.d(TAG, "Entered region. Starting ranging");
				startRanging();
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to start ranging", e);
			}
		}
	}

	@Override
	public void didExitRegion(Region region) {
		if (region.getId2() != null && region.getId3() != null) {
			WLBeacon wlBeacon = getJSONStoreManager().getMatchingBeacon(region.getId1().toString(),
					region.getId2().toInt(), region.getId3().toInt());
			if (wlBeacon != null) {
				processExitFromBeacon(wlBeacon);
			}
		} else {
			List<WLBeacon> wlBeacons = getJSONStoreManager().getMatchingBeacons(region.getId1().toString());
			for (WLBeacon wlBeacon : wlBeacons) {
				processExitFromBeacon(wlBeacon);
			}
		}
	}

	@Override
	public void didDetermineStateForRegion(int state, Region region) {
		if (state == MonitorNotifier.INSIDE && isMonitoringEnabled()) {
			try {
				startRanging();
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to start ranging", e);
			}
		}
	}

	@Override
	public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
		if (rangingStatusHandler != null && isApplicationInForeground()) {
			rangingStatusHandler.notifyRangedBeacons(beacons);
		}
		// See if trigger actions need to be fired for any of the beacons within range.
		for (Beacon beacon : beacons) {
			WLBeacon rangedBeacon = new WLBeacon(beacon);
			if (rangedBeacon.lastSeenProximity == WLProximity.Unseen) {
				continue;
			}
			List<WLBeaconTriggerAssociation> associations = getJSONStoreManager().getBeaconTriggerAssociations(
					rangedBeacon.uuid, rangedBeacon.major, rangedBeacon.minor);
			for (WLBeaconTriggerAssociation association : associations) {
				WLBeaconTrigger beaconTrigger = getJSONStoreManager().getBeaconTrigger(association.triggerName);
				switch (beaconTrigger.triggerType) {
				case Enter:
					trackBeaconStateForEnterTrigger(rangedBeacon, beaconTrigger);
					break;
				case Exit:
					trackBeaconStateForExitTrigger(rangedBeacon, beaconTrigger);
					break;
				default:
					break;
				}
			}
			WLProximity lastSeenProximity = getJSONStoreManager().getLastSeenProximity(rangedBeacon.uuid,
					rangedBeacon.major, rangedBeacon.minor);
			if (rangedBeacon.lastSeenProximity != lastSeenProximity) {
				getJSONStoreManager().setLastSeenProximity(rangedBeacon.lastSeenProximity, rangedBeacon.uuid,
						rangedBeacon.major, rangedBeacon.minor);
			}
		}
		// Start monitoring for UUID+Major+Minor regions of all beacons within the branch/store. This is required for
		// the case of multiple beacons per branch/store so as to get notified of exit from specific beacon regions.
		// This won't be required for the case of single beacon per branch.
	}

	private void trackBeaconStateForEnterTrigger(WLBeacon rangedBeacon, WLBeaconTrigger beaconTrigger) {
		WLProximity prevProximity = getJSONStoreManager().getLastSeenProximity(rangedBeacon.uuid, rangedBeacon.major,
				rangedBeacon.minor);
		WLProximity curProximity = rangedBeacon.lastSeenProximity;
		if (prevProximity == WLProximity.Unseen) {
			// transition from WLProximity.Unseen to WLProximity.Far | WLProximity.Near | WLProximity.Immediate
			if (beaconTrigger.proximityState == WLProximity.Far) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			} else if (beaconTrigger.proximityState == WLProximity.Near
					&& (curProximity == WLProximity.Near || curProximity == WLProximity.Immediate)) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			} else if (beaconTrigger.proximityState == WLProximity.Immediate && curProximity == WLProximity.Immediate) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			}
		} else if (prevProximity == WLProximity.Far
				&& (curProximity == WLProximity.Near || curProximity == WLProximity.Immediate)) {
			// transition from WLProximity.Far to WLProximity.Near | WLProximity.Immediate
			if (beaconTrigger.proximityState == WLProximity.Near) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			} else if (beaconTrigger.proximityState == WLProximity.Immediate && curProximity == WLProximity.Immediate) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			}
		} else if (prevProximity == WLProximity.Near && curProximity == WLProximity.Immediate) {
			// transition from WLProximity.Near to WLProximity.Immediate
			if (beaconTrigger.proximityState == WLProximity.Immediate) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			}
		}
	}

	private void trackBeaconStateForExitTrigger(WLBeacon rangedBeacon, WLBeaconTrigger beaconTrigger) {
		WLProximity prevProximity = getJSONStoreManager().getLastSeenProximity(rangedBeacon.uuid, rangedBeacon.major,
				rangedBeacon.minor);
		WLProximity curProximity = rangedBeacon.lastSeenProximity;
		if (prevProximity == WLProximity.Immediate
				&& (curProximity == WLProximity.Near || curProximity == WLProximity.Far)) {
			// transition from WLProximity.Immediate to WLProximity.Near | WLProximity.Far
			if (beaconTrigger.proximityState == WLProximity.Immediate) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			} else if (beaconTrigger.proximityState == WLProximity.Near && curProximity == WLProximity.Far) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			}
		} else if (prevProximity == WLProximity.Near && curProximity == WLProximity.Far) {
			// transition from WLProximity.Near to WLProximity.Far
			if (beaconTrigger.proximityState == WLProximity.Near) {
				fireTriggerAction(beaconTrigger, rangedBeacon);
			}
		}
	}

	private void processExitFromBeacon(WLBeacon wlBeacon) {
		WLProximity prevProximity = getJSONStoreManager().getLastSeenProximity(wlBeacon.uuid, wlBeacon.major,
				wlBeacon.minor);
		if (prevProximity == WLProximity.Unseen) {
			return;
		}
		List<WLBeaconTriggerAssociation> associations = getJSONStoreManager().getBeaconTriggerAssociations(
				wlBeacon.uuid, wlBeacon.major, wlBeacon.minor);
		for (WLBeaconTriggerAssociation association : associations) {
			WLBeaconTrigger beaconTrigger = getJSONStoreManager().getBeaconTrigger(association.triggerName);
			if (beaconTrigger.triggerType == WLTriggerType.Exit) {
				processExitTrigger(wlBeacon, beaconTrigger, prevProximity);
			}
		}
		getJSONStoreManager().setLastSeenProximity(WLProximity.Unseen, wlBeacon.uuid, wlBeacon.major, wlBeacon.minor);
	}

	private void processExitTrigger(WLBeacon monitoredBeacon, WLBeaconTrigger beaconTrigger, WLProximity prevProximity) {
		if (prevProximity == WLProximity.Immediate) {
			// transition from WLProximity.Immediate to WLProximity.Unseen
			fireTriggerAction(beaconTrigger, monitoredBeacon);
		} else if (prevProximity == WLProximity.Near) {
			// transition from WLProximity.Near to WLProximity.Unseen
			if (beaconTrigger.proximityState == WLProximity.Near || beaconTrigger.proximityState == WLProximity.Far) {
				fireTriggerAction(beaconTrigger, monitoredBeacon);
			}
		} else if (prevProximity == WLProximity.Far) {
			// transition from WLProximity.Far to WLProximity.Unseen
			if (beaconTrigger.proximityState == WLProximity.Far) {
				fireTriggerAction(beaconTrigger, monitoredBeacon);
			}
		}
	}

	public void resetMonitoringRangingState() {
		for (WLBeacon wlBeacon : getJSONStoreManager().getBeaconsFromJsonStore()) {
			getJSONStoreManager().setLastSeenProximity(WLProximity.Unseen, wlBeacon.uuid, wlBeacon.major,
					wlBeacon.minor);
		}
	}

	private void fireTriggerAction(WLBeaconTrigger beaconTrigger, WLBeacon wlBeacon) {
		String alertTitle = beaconTrigger.triggerType.toString() + " " + beaconTrigger.proximityState.toString();
		String alertMessage = beaconTrigger.actionPayload.get("alert");
		String branchName = getBranchName(wlBeacon.uuid, wlBeacon.major, wlBeacon.minor);
		if (branchName != null) {
			alertMessage = alertMessage.replaceAll("\\$branchName", branchName);
		}
		if (alertHandler != null && isApplicationInForeground()) {
			alertHandler.showAlert(alertTitle, alertMessage, false);
		} else {
			sendLocalNotification(alertTitle, alertMessage);
		}
	}

	private String getBranchName(String uuid, int major, int minor) {
		WLBeacon wlBeacon = getJSONStoreManager().getMatchingBeacon(uuid, major, minor);
		if (wlBeacon.customData != null && wlBeacon.customData.containsKey("branchName")) {
			return wlBeacon.customData.get("branchName");
		}
		return null;
	}

	public static interface WLRangingStatusHandler {
		public void notifyRangedBeacons(Collection<Beacon> beacons);
	}

	public void setRangingStatusHandler(WLRangingStatusHandler rangingStatusHandler) {
		this.rangingStatusHandler = rangingStatusHandler;
	}

	public static class WLAlertHandler {
		private Activity activity;

		public WLAlertHandler(Activity activity) {
			this.activity = activity;
		}

		public void showAlert(final String alertTitle, final String alertMessage, final boolean closeApp) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(alertTitle);
			builder.setMessage(alertMessage);
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (closeApp) {
						// finish();
						System.exit(0);
					}
				}
			});
			activity.runOnUiThread(new Runnable() {
				public void run() {
					builder.show();
				}
			});
		}
	}

	public void setAlertHandler(WLAlertHandler alertHandler) {
		this.alertHandler = alertHandler;
	}

	private void sendLocalNotification(final String alertTitle, final String alertMessage) {
		Context context = this.getApplicationContext();
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
				.setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle(alertTitle)
				.setContentText(alertMessage).setDefaults(Notification.DEFAULT_ALL).setAutoCancel(true);
		// Creates an explicit intent for an Activity in your app

		Intent launchIntent = getPackageManager().getLaunchIntentForPackage(context.getPackageName());
		launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		launchIntent.putExtra("alertTitle", alertTitle);
		launchIntent.putExtra("alertMessage", alertMessage);

		// The stack builder object will contain an artificial back stack for the started Activity.
		// This ensures that navigating backward from the Activity leads out of your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack for the Intent (but not the Intent itself)
		// stackBuilder.addParentStack(getNotificationActionClass());
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(launchIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(resultPendingIntent);

		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		int mId = random.nextInt();
		mNotificationManager.notify(mId, mBuilder.build());
	}

	private static Random random = new Random();

	public boolean isApplicationInForeground() {
		// http://stackoverflow.com/questions/3667022/checking-if-an-android-application-is-running-in-the-background
		return activityLifecycleHandler.resumed > activityLifecycleHandler.paused;
	}

	public static class ActivityLifecycleHandler implements ActivityLifecycleCallbacks {
		private int resumed;
		private int paused;

		@Override
		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		}

		@Override
		public void onActivityDestroyed(Activity activity) {
		}

		@Override
		public void onActivityResumed(Activity activity) {
			++resumed;
		}

		@Override
		public void onActivityPaused(Activity activity) {
			++paused;
		}

		@Override
		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		}

		@Override
		public void onActivityStarted(Activity activity) {
		}

		@Override
		public void onActivityStopped(Activity activity) {
		}
	}

	public static final String PREFS_FILE_NAME = "WLBeaconsAndTriggersPrefs";
	public static final String PREF_NAME = "MonitoringEnabled";

	public boolean isMonitoringEnabled() {
		SharedPreferences sharedPreferences = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		return sharedPreferences.getBoolean(PREF_NAME, false);
	}

	public void setMonitoringEnabled(boolean enabled) {
		SharedPreferences.Editor editor = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).edit();
		editor.putBoolean(PREF_NAME, enabled);
		editor.commit();
	}
}
