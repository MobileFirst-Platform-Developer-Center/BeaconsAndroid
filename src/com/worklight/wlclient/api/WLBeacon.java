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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.altbeacon.beacon.Beacon;
import org.json.JSONException;
import org.json.JSONObject;

import com.worklight.wlclient.api.WLBeaconTrigger.WLProximity;

public class WLBeacon {
	String uuid;
	int major;
	int minor;
	double latitude;
	double longitude;
	Map<String, String> customData = null;
	WLProximity lastSeenProximity = WLProximity.Unseen;

	public WLBeacon(JSONObject beaconJson) {
		try {
			uuid = beaconJson.getString("uuid");
			major = beaconJson.getInt("major");
			minor = beaconJson.getInt("minor");
			if (beaconJson.has("latitude")) {
				latitude = beaconJson.getDouble("latitude");
			}
			if (beaconJson.has("longitude")) {
				longitude = beaconJson.getDouble("longitude");
			}
			if (beaconJson.has("customData")) {
				JSONObject customDataJson = beaconJson.getJSONObject("customData");
				if (customDataJson != null) {
					customData = new HashMap<String, String>();
					for (Iterator<String> keys = customDataJson.keys(); keys.hasNext();) {
						String key = keys.next();
						String value = customDataJson.getString(key);
						customData.put(key, value);
					}
				}
			}
			if (beaconJson.has("lastSeenProximity")) {
				lastSeenProximity = WLProximity.valueOf(beaconJson.getString("lastSeenProximity"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public WLBeacon(Beacon beacon) {
		uuid = beacon.getId1().toString();
		major = beacon.getId2().toInt();
		minor = beacon.getId3().toInt();
		lastSeenProximity = convertDistanceToProximity(beacon.getDistance());
	}

	private WLProximity convertDistanceToProximity(double distance) {
		// http://developer.radiusnetworks.com/2014/12/04/fundamentals-of-beacon-ranging.html
		// Experimentation shows that 'immediate' means about 0.5 meters or less,
		// 'near' means about three meters or less, and 'far' means more than three meters.
		if (distance < 0.5) {
			return WLProximity.Immediate;
		} else if (distance < 3) {
			return WLProximity.Near;
		} else {
			return WLProximity.Far;
		}
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("UUID: " + uuid);
		stringBuilder.append(", major: " + major);
		stringBuilder.append(", minor: " + minor);
		if (latitude > 0) {
			stringBuilder.append(", latitude: " + latitude);
		}
		if (longitude > 0) {
			stringBuilder.append(", longitude: " + longitude);
		}
		if (customData != null) {
			for (String key : customData.keySet()) {
				String value = customData.get(key);
				stringBuilder.append(", " + key + ": '" + value + "'");
			}
		}
		stringBuilder.append(", lastSeenProximity: " + lastSeenProximity);
		return stringBuilder.toString();
	}
}
