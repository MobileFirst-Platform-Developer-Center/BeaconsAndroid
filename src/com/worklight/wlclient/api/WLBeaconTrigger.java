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

import org.json.JSONException;
import org.json.JSONObject;

public class WLBeaconTrigger {

	public enum WLTriggerType {
		Enter, Exit, DwellInside, DwellOutside
	}

	public enum WLProximity {
		Immediate, Near, Far, Unseen
	}

	String triggerName;
	WLTriggerType triggerType;
	WLProximity proximityState;
	int dwellingTime;
	Map<String, String> actionPayload;

	public WLBeaconTrigger(JSONObject triggerJson) {
		try {
			triggerName = triggerJson.getString("triggerName");
			triggerType = WLTriggerType.valueOf(triggerJson.getString("triggerType"));
			proximityState = WLProximity.valueOf(triggerJson.getString("proximityState"));
			if (triggerJson.has("dwellingTime")) {
				dwellingTime = triggerJson.getInt("dwellingTime");
			}
			if (triggerJson.has("actionPayload")) {
				JSONObject actionPayloadJson = triggerJson.getJSONObject("actionPayload");
				if (actionPayloadJson != null) {
					actionPayload = new HashMap<String, String>();
					for (Iterator<String> keys = actionPayloadJson.keys(); keys.hasNext();) {
						String key = keys.next();
						String value = actionPayloadJson.getString(key);
						actionPayload.put(key, value);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("TriggerName: " + triggerName);
		stringBuilder.append(", triggerType: " + triggerType);
		stringBuilder.append(", proximityState: " + proximityState);
		if (dwellingTime > 0) {
			stringBuilder.append(", dwellingTime: " + dwellingTime);
		}
		if (actionPayload != null) {
			for (String key : actionPayload.keySet()) {
				String value = actionPayload.get(key);
				stringBuilder.append(", " + key + ": '" + value + "'");
			}
		}
		return stringBuilder.toString();
	}
}
