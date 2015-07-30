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
