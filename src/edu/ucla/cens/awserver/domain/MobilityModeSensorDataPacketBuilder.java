package edu.ucla.cens.awserver.domain;

import org.json.JSONObject;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.JsonUtils;

/**
 * @author selsky
 */
public class MobilityModeSensorDataPacketBuilder extends AbstractDataPacketBuilder {

	public MobilityModeSensorDataPacketBuilder() {
		
	}
	
	public MetadataDataPacket createDataPacketFrom(JSONObject source, AwRequest awRequest) {
		MobilitySensorDataPacket packet = new MobilitySensorDataPacket();
		createCommonFields(source, packet);
		packet.setMode(JsonUtils.getStringFromJsonObject(source, "mode"));
		String sensorDataString = JsonUtils.getJsonObjectFromJsonObject(source, "sensor_data").toString();
		packet.setSensorDataString(sensorDataString);
		return packet;
	}
}
