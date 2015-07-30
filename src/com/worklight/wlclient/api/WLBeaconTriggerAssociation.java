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

import org.json.JSONException;
import org.json.JSONObject;

public class WLBeaconTriggerAssociation {
	String uuid;
	int major;
	int minor;
	String triggerName;

	public WLBeaconTriggerAssociation(JSONObject associationJson) {
		try {
			uuid = associationJson.getString("uuid");
			major = associationJson.getInt("major");
			minor = associationJson.getInt("minor");
			triggerName = associationJson.getString("triggerName");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("UUID: " + uuid);
		stringBuilder.append(", major: " + major);
		stringBuilder.append(", minor: " + minor);
		stringBuilder.append(", triggerName: " + triggerName);
		return stringBuilder.toString();
	}
}
