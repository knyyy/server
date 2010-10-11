package edu.ucla.cens.awserver.domain;

import org.json.JSONObject;

import edu.ucla.cens.awserver.util.JsonUtils;

/**
 * Abstract helper class for handling fields common to all packets.
 * 
 * @author selsky
 */
public abstract class AbstractDataPacketBuilder implements DataPacketBuilder {
	
	/**
	 * Sets the values for the fields common to all packets: date, time (millis since epoch), timezone, latitude, longitude, 
	 * accuracy, provider, client.
	 */
	public void createCommonFields(JSONObject source, MetadataDataPacket packet) {
		String date = JsonUtils.getStringFromJsonObject(source, "date");
		long time = JsonUtils.getLongFromJsonObject(source, "time");
		String timezone = JsonUtils.getStringFromJsonObject(source, "timezone");
		JSONObject location = JsonUtils.getJsonObjectFromJsonObject(source, "location");
		Double latitude = checkForDoubleNaN(location, "latitude");
		Double longitude = checkForDoubleNaN(location, "longitude");
		Double accuracy = JsonUtils.getDoubleFromJsonObject(location, "accuracy");
		String provider = JsonUtils.getStringFromJsonObject(location, "provider");
		packet.setDate(date);
		packet.setEpochTime(time);
		packet.setTimezone(timezone);
		packet.setLatitude(latitude);
		packet.setLongitude(longitude);
		packet.setAccuracy(accuracy);
		packet.setProvider(provider);
	}
	
	private Double checkForDoubleNaN(JSONObject source, String key) {
		// latitude and longitude may be sent with the string value "Double.NaN"
		Double value = JsonUtils.getDoubleFromJsonObject(source, key);
		
		return null == value? Double.NaN : value;
	}
}
