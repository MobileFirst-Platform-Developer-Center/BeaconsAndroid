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
