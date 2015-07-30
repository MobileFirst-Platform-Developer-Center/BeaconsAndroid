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
