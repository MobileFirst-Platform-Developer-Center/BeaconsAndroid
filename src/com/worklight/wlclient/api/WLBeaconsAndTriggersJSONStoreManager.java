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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.worklight.jsonstore.api.JSONStoreCollection;
import com.worklight.jsonstore.api.JSONStoreInitOptions;
import com.worklight.jsonstore.api.JSONStoreQueryPart;
import com.worklight.jsonstore.api.JSONStoreQueryParts;
import com.worklight.jsonstore.api.WLJSONStore;
import com.worklight.jsonstore.database.SearchFieldType;
import com.worklight.jsonstore.exceptions.JSONStoreAddException;
import com.worklight.jsonstore.exceptions.JSONStoreCloseAllException;
import com.worklight.jsonstore.exceptions.JSONStoreDatabaseClosedException;
import com.worklight.jsonstore.exceptions.JSONStoreDestroyFailureException;
import com.worklight.jsonstore.exceptions.JSONStoreFileAccessException;
import com.worklight.jsonstore.exceptions.JSONStoreFilterException;
import com.worklight.jsonstore.exceptions.JSONStoreFindException;
import com.worklight.jsonstore.exceptions.JSONStoreInvalidPasswordException;
import com.worklight.jsonstore.exceptions.JSONStoreInvalidSchemaException;
import com.worklight.jsonstore.exceptions.JSONStoreMigrationException;
import com.worklight.jsonstore.exceptions.JSONStoreRemoveException;
import com.worklight.jsonstore.exceptions.JSONStoreReplaceException;
import com.worklight.jsonstore.exceptions.JSONStoreSchemaMismatchException;
import com.worklight.jsonstore.exceptions.JSONStoreTransactionDuringInitException;
import com.worklight.jsonstore.exceptions.JSONStoreTransactionFailureException;
import com.worklight.wlclient.api.WLBeaconTrigger.WLProximity;

public class WLBeaconsAndTriggersJSONStoreManager {
	private Context context = null;

	private static WLBeaconsAndTriggersJSONStoreManager sharedInstance = null;

	private WLBeaconsAndTriggersJSONStoreManager(Context context) {
		this.context = context;
	}

	public static WLBeaconsAndTriggersJSONStoreManager getInstance(Context context) {
		if (sharedInstance == null) {
			sharedInstance = new WLBeaconsAndTriggersJSONStoreManager(context);
		}
		return sharedInstance;
	}

	private class InvokeProcedureResponseListener implements WLResponseListener {
		private WLResponseListener responseListenerDelegate;

		public InvokeProcedureResponseListener(WLResponseListener listener) {
			this.responseListenerDelegate = listener;
		}

		public void onSuccess(WLResponse response) {
			JSONObject responseJson = response.getResponseJSON();
			saveBeaconsAndTriggersIntoJsonStore(responseJson);
			responseListenerDelegate.onSuccess(response);
		}

		public void onFailure(WLFailResponse response) {
			responseListenerDelegate.onFailure(response);
		}
	}

	public void loadBeaconsAndTriggers(String adapterName, String procedureName, WLResponseListener responseListener) {
		try {
			URI adapterPath = new URI("/adapters/" + adapterName + "/" + procedureName);
			WLResourceRequest request = new WLResourceRequest(adapterPath, WLResourceRequest.GET);
			request.setQueryParameter("params", "['" + getApplicationName() + "',null]");
			request.send(new InvokeProcedureResponseListener(responseListener));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	private static String applicationName = null;

	private String getApplicationName() {
		if (applicationName == null) {
			try {
				InputStream inputStream = context.getAssets().open("wlclient.properties");
				Properties properties = new Properties();
				properties.load(inputStream);
				applicationName = properties.getProperty("wlAppId");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return applicationName;
	}

	private void saveBeaconsAndTriggersIntoJsonStore(JSONObject responseJson) {
		try {
			JSONArray beaconsJsonArray = responseJson.getJSONArray("beacons");
			JSONArray triggersJsonArray = responseJson.getJSONArray("beaconTriggers");
			JSONArray associationsJsonArray = responseJson.getJSONArray("beaconTriggerAssociations");

			try {
				WLJSONStore.getInstance(context).destroy();
			} catch (JSONStoreDestroyFailureException e) {
				e.printStackTrace();
			} catch (JSONStoreTransactionFailureException e) {
				e.printStackTrace();
			}

			saveBeaconsIntoJsonStore(beaconsJsonArray);
			saveBeaconTriggersIntoJsonStore(triggersJsonArray);
			saveBeaconTriggerAssociationsIntoJsonStore(associationsJsonArray);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void saveBeaconsIntoJsonStore(JSONArray beaconsJsonArray) {
		try {
			getBeaconsJSONStoreCollection().addData(beaconsJsonArray);
		} catch (JSONStoreAddException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
	}

	private void saveBeaconTriggersIntoJsonStore(JSONArray triggersJsonArray) {
		// Workaround for "triggerName cannot be used as search field as it is a reserved keyword in SQLite!"
		JSONArray newTriggersJsonArray = new JSONArray();
		for (int i = 0; i < triggersJsonArray.length(); i++) {
			try {
				JSONObject triggersJson = triggersJsonArray.getJSONObject(i);
				String triggerName = triggersJson.getString("triggerName");
				triggersJson.put("name", triggerName);
				newTriggersJsonArray.put(triggersJson);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		try {
			getTriggersJSONStoreCollection().addData(newTriggersJsonArray);
		} catch (JSONStoreAddException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
	}

	private void saveBeaconTriggerAssociationsIntoJsonStore(JSONArray associationsJsonArray) {
		try {
			getAssociationsJSONStoreCollection().addData(associationsJsonArray);
		} catch (JSONStoreAddException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
	}

	private JSONStoreCollection getBeaconsJSONStoreCollection() throws JSONStoreInvalidSchemaException {
		String collectionName = "beacons";
		JSONStoreCollection jsonStoreCollection = WLJSONStore.getInstance(context).getCollectionByName(collectionName);
		if (jsonStoreCollection == null) {
			jsonStoreCollection = new JSONStoreCollection(collectionName);
			jsonStoreCollection.setSearchField("uuid", SearchFieldType.STRING);
			jsonStoreCollection.setSearchField("major", SearchFieldType.INTEGER);
			jsonStoreCollection.setSearchField("minor", SearchFieldType.INTEGER);
			openJSONStoreCollection(jsonStoreCollection);
		}
		return jsonStoreCollection;
	}

	private JSONStoreCollection getTriggersJSONStoreCollection() throws JSONStoreInvalidSchemaException {
		String collectionName = "beaconTriggers";
		JSONStoreCollection jsonStoreCollection = WLJSONStore.getInstance(context).getCollectionByName(collectionName);
		if (jsonStoreCollection == null) {
			jsonStoreCollection = new JSONStoreCollection(collectionName);
			// triggerName cannot be used as search field as it is a reserved keyword in SQLite!
			jsonStoreCollection.setSearchField("name", SearchFieldType.STRING);
			openJSONStoreCollection(jsonStoreCollection);
		}
		return jsonStoreCollection;
	}

	private JSONStoreCollection getAssociationsJSONStoreCollection() throws JSONStoreInvalidSchemaException {
		String collectionName = "beaconTriggerAssociations";
		JSONStoreCollection jsonStoreCollection = WLJSONStore.getInstance(context).getCollectionByName(collectionName);
		if (jsonStoreCollection == null) {
			jsonStoreCollection = new JSONStoreCollection(collectionName);
			jsonStoreCollection.setSearchField("uuid", SearchFieldType.STRING);
			jsonStoreCollection.setSearchField("major", SearchFieldType.INTEGER);
			jsonStoreCollection.setSearchField("minor", SearchFieldType.INTEGER);
			openJSONStoreCollection(jsonStoreCollection);
		}
		return jsonStoreCollection;
	}

	private JSONStoreCollection getMonitoredRegionsJSONStoreCollection() throws JSONStoreInvalidSchemaException {
		String collectionName = "monitoredRegions";
		JSONStoreCollection jsonStoreCollection = WLJSONStore.getInstance(context).getCollectionByName(collectionName);
		if (jsonStoreCollection == null) {
			jsonStoreCollection = new JSONStoreCollection(collectionName);
			jsonStoreCollection.setSearchField("identifier", SearchFieldType.STRING);
			openJSONStoreCollection(jsonStoreCollection);
		}
		return jsonStoreCollection;
	}

	private void openJSONStoreCollection(JSONStoreCollection jsonStoreCollection) {
		// Optional options object.
		JSONStoreInitOptions initOptions = new JSONStoreInitOptions();
		// Optional username, default 'jsonstore'.
		// initOptions.setUsername("jsonstore");
		// Optional password, default no password.
		// initOptions.setPassword("password");

		// Open the collection.
		List<JSONStoreCollection> collections = new LinkedList<JSONStoreCollection>();
		collections.add(jsonStoreCollection);
		try {
			WLJSONStore.getInstance(context).openCollections(collections, initOptions);
		} catch (JSONStoreInvalidSchemaException | JSONStoreFileAccessException | JSONStoreMigrationException
				| JSONStoreCloseAllException | JSONStoreInvalidPasswordException | JSONStoreSchemaMismatchException
				| JSONStoreTransactionDuringInitException e) {
			e.printStackTrace();
		}
	}

	public List<WLBeacon> getBeaconsFromJsonStore() {
		ArrayList<WLBeacon> beacons = new ArrayList<WLBeacon>();
		try {
			List<JSONObject> beaconsJson = getBeaconsJSONStoreCollection().findAllDocuments();
			for (JSONObject beaconData : beaconsJson) {
				try {
					JSONObject beaconJson = beaconData.getJSONObject("json");
					WLBeacon wlBeacon = new WLBeacon(beaconJson);
					beacons.add(wlBeacon);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
		return beacons;
	}

	public List<WLBeaconTrigger> getBeaconTriggersFromJsonStore() {
		ArrayList<WLBeaconTrigger> triggers = new ArrayList<WLBeaconTrigger>();
		try {
			List<JSONObject> triggersJson = getTriggersJSONStoreCollection().findAllDocuments();
			for (JSONObject triggerData : triggersJson) {
				try {
					JSONObject triggerJson = triggerData.getJSONObject("json");
					WLBeaconTrigger wlBeaconTrigger = new WLBeaconTrigger(triggerJson);
					triggers.add(wlBeaconTrigger);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
		return triggers;
	}

	public List<WLBeaconTriggerAssociation> getBeaconTriggerAssociationsFromJsonStore() {
		ArrayList<WLBeaconTriggerAssociation> associations = new ArrayList<WLBeaconTriggerAssociation>();
		try {
			List<JSONObject> associationsJson = getAssociationsJSONStoreCollection().findAllDocuments();
			for (JSONObject associationData : associationsJson) {
				try {
					JSONObject associationJson = associationData.getJSONObject("json");
					WLBeaconTriggerAssociation wlBeaconTriggerAssociation = new WLBeaconTriggerAssociation(
							associationJson);
					associations.add(wlBeaconTriggerAssociation);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
		return associations;
	}

	public Set<String> getUuids() {
		Set<String> uuids = new HashSet<String>();
		try {
			List<JSONObject> beaconsJson = getBeaconsJSONStoreCollection().findAllDocuments();
			for (JSONObject beaconData : beaconsJson) {
				try {
					JSONObject beaconJson = beaconData.getJSONObject("json");
					String uuid = beaconJson.getString("uuid");
					uuids.add(uuid);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
		return uuids;
	}

	private List<WLBeacon> getMatchingBeacons(JSONStoreQueryPart jsonStoreQueryPart) {
		ArrayList<WLBeacon> beacons = new ArrayList<WLBeacon>();
		try {
			JSONStoreQueryParts jsonStoreQueryParts = new JSONStoreQueryParts();
			jsonStoreQueryParts.addQueryPart(jsonStoreQueryPart);
			List<JSONObject> beaconsJson = getBeaconsJSONStoreCollection().findDocuments(jsonStoreQueryParts);
			for (JSONObject beaconData : beaconsJson) {
				try {
					JSONObject beaconJson = beaconData.getJSONObject("json");
					WLBeacon wlBeacon = new WLBeacon(beaconJson);
					beacons.add(wlBeacon);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException
				| JSONStoreFilterException e) {
			e.printStackTrace();
		}
		return beacons;
	}

	public List<WLBeacon> getMatchingBeacons(String uuid) {
		JSONStoreQueryPart jsonStoreQueryPart = new JSONStoreQueryPart();
		jsonStoreQueryPart.addEqual("uuid", uuid);
		return getMatchingBeacons(jsonStoreQueryPart);
	}

	public List<WLBeacon> getMatchingBeacons(String uuid, int major) {
		JSONStoreQueryPart jsonStoreQueryPart = new JSONStoreQueryPart();
		jsonStoreQueryPart.addEqual("uuid", uuid);
		jsonStoreQueryPart.addEqual("major", major);
		return getMatchingBeacons(jsonStoreQueryPart);
	}

	public WLBeacon getMatchingBeacon(String uuid, int major, int minor) {
		JSONStoreQueryPart jsonStoreQueryPart = new JSONStoreQueryPart();
		jsonStoreQueryPart.addEqual("uuid", uuid);
		jsonStoreQueryPart.addEqual("major", major);
		jsonStoreQueryPart.addEqual("minor", minor);
		List<WLBeacon> beacons = getMatchingBeacons(jsonStoreQueryPart);
		if (beacons.size() == 1) {
			return beacons.get(0);
		}
		return null;
	}

	public List<WLBeaconTriggerAssociation> getBeaconTriggerAssociations(String uuid, int major, int minor) {
		ArrayList<WLBeaconTriggerAssociation> associations = new ArrayList<WLBeaconTriggerAssociation>();
		try {
			JSONStoreQueryParts jsonStoreQueryParts = new JSONStoreQueryParts();
			JSONStoreQueryPart jsonStoreQueryPart = new JSONStoreQueryPart();
			jsonStoreQueryPart.addEqual("uuid", uuid);
			jsonStoreQueryPart.addEqual("major", major);
			jsonStoreQueryPart.addEqual("minor", minor);
			jsonStoreQueryParts.addQueryPart(jsonStoreQueryPart);
			List<JSONObject> associationsJson = getAssociationsJSONStoreCollection().findDocuments(jsonStoreQueryParts);
			for (JSONObject associationData : associationsJson) {
				try {
					JSONObject associationJson = associationData.getJSONObject("json");
					WLBeaconTriggerAssociation wlBeaconTriggerAssociation = new WLBeaconTriggerAssociation(
							associationJson);
					associations.add(wlBeaconTriggerAssociation);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException
				| JSONStoreFilterException e) {
			e.printStackTrace();
		}
		return associations;
	}

	public WLBeaconTrigger getBeaconTrigger(String triggerName) {
		try {
			JSONStoreQueryParts jsonStoreQueryParts = new JSONStoreQueryParts();
			JSONStoreQueryPart jsonStoreQueryPart = new JSONStoreQueryPart();
			// triggerName cannot be used as search field as it is a reserved keyword in SQLite!
			jsonStoreQueryPart.addEqual("name", triggerName);
			jsonStoreQueryParts.addQueryPart(jsonStoreQueryPart);
			List<JSONObject> triggersJson = getTriggersJSONStoreCollection().findDocuments(jsonStoreQueryParts);
			if (triggersJson != null && triggersJson.size() == 1) {
				JSONObject triggerJson = triggersJson.get(0).getJSONObject("json");
				WLBeaconTrigger wlBeaconTrigger = new WLBeaconTrigger(triggerJson);
				return wlBeaconTrigger;
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException
				| JSONStoreFilterException | JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private JSONObject toJSON(Region region) throws JSONException {
		JSONObject regionJson = new JSONObject();
		regionJson.put("identifier", region.getUniqueId());
		if (region.getId1() != null) {
			regionJson.put("uuid", region.getId1().toHexString());
		}
		if (region.getId2() != null) {
			regionJson.put("major", region.getId2().toInt());
		}
		if (region.getId3() != null) {
			regionJson.put("minor", region.getId3().toInt());
		}
		return regionJson;
	}

	private Region toRegion(JSONObject regionJson) throws JSONException {
		String uniqueId = regionJson.getString("identifier");
		Identifier id1 = null;
		Identifier id2 = null;
		Identifier id3 = null;
		if (regionJson.has("name")) {
			String uuid = regionJson.getString("uuid");
			id1 = Identifier.parse(uuid);
		}
		if (regionJson.has("major")) {
			int major = regionJson.getInt("major");
			id2 = Identifier.fromInt(major);
		}
		if (regionJson.has("minor")) {
			int minor = regionJson.getInt("minor");
			id3 = Identifier.fromInt(minor);
		}
		Region region = new Region(uniqueId, id1, id2, id3);
		return region;
	}

	public void addToMonitoredRegion(Region region) {
		try {
			JSONObject regionJson = toJSON(region);
			getMonitoredRegionsJSONStoreCollection().addData(regionJson);
		} catch (JSONException | JSONStoreAddException | JSONStoreDatabaseClosedException
				| JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
	}

	public void removeFromMonitoredRegions(Region region) {
		try {
			JSONStoreQueryParts jsonStoreQueryParts = new JSONStoreQueryParts();
			JSONStoreQueryPart jsonStoreQueryPart = new JSONStoreQueryPart();
			jsonStoreQueryPart.addEqual("identifier", region.getUniqueId());
			jsonStoreQueryParts.addQueryPart(jsonStoreQueryPart);
			List<JSONObject> monitoredRegionsJson = getMonitoredRegionsJSONStoreCollection().findDocuments(
					jsonStoreQueryParts);
			if (monitoredRegionsJson != null && monitoredRegionsJson.size() == 1) {
				Integer _id = monitoredRegionsJson.get(0).getInt("_id");
				getMonitoredRegionsJSONStoreCollection().removeDocumentById(_id);
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException
				| JSONStoreFilterException | JSONException | JSONStoreRemoveException e) {
			e.printStackTrace();
		}
	}

	public List<Region> getMonitoredRegions() {
		ArrayList<Region> monitoredRegions = new ArrayList<Region>();
		try {
			List<JSONObject> monitoredRegionsJson = getMonitoredRegionsJSONStoreCollection().findAllDocuments();
			for (JSONObject monitoredRegionData : monitoredRegionsJson) {
				try {
					JSONObject monitoredRegionJson = monitoredRegionData.getJSONObject("json");
					Region region = toRegion(monitoredRegionJson);
					monitoredRegions.add(region);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException e) {
			e.printStackTrace();
		}
		return monitoredRegions;
	}

	public void setLastSeenProximity(WLProximity proximity, String uuid, int major, int minor) {
		try {
			JSONStoreQueryParts jsonStoreQueryParts = new JSONStoreQueryParts();
			JSONStoreQueryPart jsonStoreQueryPart = new JSONStoreQueryPart();
			jsonStoreQueryPart.addEqual("uuid", uuid);
			jsonStoreQueryPart.addEqual("major", major);
			jsonStoreQueryPart.addEqual("minor", minor);
			jsonStoreQueryParts.addQueryPart(jsonStoreQueryPart);
			List<JSONObject> beaconsJson = getBeaconsJSONStoreCollection().findDocuments(jsonStoreQueryParts);
			if (beaconsJson != null && beaconsJson.size() == 1) {
				JSONObject beaconData = beaconsJson.get(0);
				JSONObject beaconJson = beaconData.getJSONObject("json");
				beaconJson.put("lastSeenProximity", proximity.toString());
				getBeaconsJSONStoreCollection().replaceDocument(beaconData);
			}
		} catch (JSONStoreFindException | JSONStoreDatabaseClosedException | JSONStoreInvalidSchemaException
				| JSONStoreFilterException | JSONException | JSONStoreReplaceException e) {
			e.printStackTrace();
		}
	}

	public WLProximity getLastSeenProximity(String uuid, int major, int minor) {
		WLBeacon wlBeacon = getMatchingBeacon(uuid, major, minor);
		if (wlBeacon != null) {
			return wlBeacon.lastSeenProximity;
		}
		return WLProximity.Unseen;
	}
}
